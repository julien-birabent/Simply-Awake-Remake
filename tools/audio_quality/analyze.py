#!/usr/bin/env python3
"""Measure the audio quality of every Simply Awake track and derive a GOOD / FAIR / POOR level.

Stage 1 (ffmpeg + numpy): loudness, clipping, noise floor, bandwidth, clicks.
Stage 2 (--balance, demucs): voice / music balance on excerpts.
"""

import argparse
import csv
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import numpy as np

SAMPLE_RATE = 44100
SILENCE_DB = -85.0

DEFAULT_THRESHOLDS = {
    "voiceMusicBalanceDb": {"direction": "higher", "good": 0.0, "fair": -99.0},
    "crestDb": {"direction": "higher", "good": 16.0, "fair": 11.0},
    "clippedRunsPerMinute": {"direction": "lower", "good": 1.0, "fair": 10.0},
}

LEVEL_RANK = {"GOOD": 0, "FAIR": 1, "POOR": 2}


def log(message):
    print(message, file=sys.stderr, flush=True)


def run(cmd, **kwargs):
    return subprocess.run(cmd, check=True, capture_output=True, **kwargs)


def fetch_catalog(base_url, out_dir):
    url = base_url.rstrip("/") + "/api/tracks"
    log(f"GET {url}")
    with urllib.request.urlopen(url, timeout=60) as response:
        tracks = json.load(response)
    (out_dir / "catalog.json").write_text(json.dumps(tracks, indent=2, ensure_ascii=False))
    return tracks


def track_url(base_url, track_id):
    return base_url.rstrip("/") + f"/media/audio/{track_id}.mp3"


def remote_size(base_url, track_id):
    request = urllib.request.Request(track_url(base_url, track_id), method="HEAD")
    with urllib.request.urlopen(request, timeout=60) as response:
        return int(response.headers.get("Content-Length", 0))


def download(base_url, track_id, cache_dir, expected_size=None):
    target = cache_dir / f"{track_id}.mp3"
    if target.exists() and target.stat().st_size > 0:
        if expected_size is None or target.stat().st_size == expected_size:
            return target
        log(f"cache outdated {track_id}: {target.stat().st_size} != {expected_size} bytes")
        target.unlink()
    url = track_url(base_url, track_id)
    log(f"download {url}")
    tmp = target.with_suffix(".part")
    with urllib.request.urlopen(url, timeout=120) as response, open(tmp, "wb") as handle:
        shutil.copyfileobj(response, handle)
    tmp.rename(target)
    return target


def probe(path):
    result = run(["ffprobe", "-v", "error", "-print_format", "json", "-show_format", "-show_streams", str(path)])
    info = json.loads(result.stdout)
    audio = next(stream for stream in info["streams"] if stream["codec_type"] == "audio")
    fmt = info["format"]
    return {
        "codec": audio.get("codec_name"),
        "bitrateKbps": round(int(fmt.get("bit_rate", 0)) / 1000),
        "sampleRateHz": int(audio.get("sample_rate", 0)),
        "channels": int(audio.get("channels", 0)),
        "durationSeconds": round(float(fmt.get("duration", 0))),
        "fileSizeMb": round(int(fmt.get("size", 0)) / 1_000_000, 1),
        "fileBytes": int(fmt.get("size", 0)),
    }


def loudness(path):
    result = subprocess.run(
        ["ffmpeg", "-nostats", "-hide_banner", "-i", str(path), "-af", "ebur128=peak=true", "-f", "null", "-"],
        capture_output=True, text=True, check=True,
    )
    summary = result.stderr[result.stderr.rfind("Summary:"):]

    def grab(pattern):
        match = re.search(pattern, summary)
        return float(match.group(1)) if match else None

    return {
        "integratedLufs": grab(r"I:\s+(-?[\d.]+) LUFS"),
        "loudnessRangeLu": grab(r"LRA:\s+(-?[\d.]+) LU"),
        "truePeakDbfs": grab(r"Peak:\s+(-?[\d.]+) dBFS"),
    }


def decode(path, start=None, duration=None):
    cmd = ["ffmpeg", "-nostats", "-hide_banner", "-v", "error"]
    if start is not None:
        cmd += ["-ss", str(start)]
    cmd += ["-i", str(path)]
    if duration is not None:
        cmd += ["-t", str(duration)]
    cmd += ["-ac", "2", "-ar", str(SAMPLE_RATE), "-f", "f32le", "-"]
    result = run(cmd)
    channels = np.frombuffer(result.stdout, dtype=np.float32).reshape(-1, 2)
    return channels.mean(axis=1), channels


def to_db(values):
    return 20 * np.log10(np.maximum(values, 1e-9))


def frame_rms_db(signal, frame, hop):
    if len(signal) < frame:
        return np.array([])
    count = (len(signal) - frame) // hop + 1
    index = np.arange(frame)[None, :] + hop * np.arange(count)[:, None]
    frames = signal[index]
    return to_db(np.sqrt(np.mean(frames * frames, axis=1)))


def clipping(channels, minutes):
    clipped = np.any(np.abs(channels) >= 0.99, axis=1)
    edges = np.diff(np.concatenate(([0], clipped.astype(np.int8), [0])))
    starts = np.flatnonzero(edges == 1)
    ends = np.flatnonzero(edges == -1)
    runs = int(np.sum((ends - starts) >= 2))
    return {
        "clippingRatio": round(float(np.mean(clipped)), 6),
        "clippedRunsPerMinute": round(runs / minutes, 2),
    }


def noise_floor(signal):
    levels = frame_rms_db(signal, frame=int(0.4 * SAMPLE_RATE), hop=int(0.2 * SAMPLE_RATE))
    audible = levels[levels > SILENCE_DB]
    if len(audible) == 0:
        return {"noiseFloorDb": None, "rmsDb": None, "silenceRatio": 1.0}
    floor = float(np.percentile(audible, 5))
    typical = float(np.percentile(audible, 50))
    return {
        "noiseFloorDb": round(floor, 1) if typical - floor >= 10 else None,
        "rmsDb": round(typical, 1),
        "silenceRatio": round(float(1 - len(audible) / len(levels)), 3),
    }


def average_spectrum(signal, window=8192, max_frames=300):
    positions = np.linspace(0, len(signal) - window, num=min(max_frames, len(signal) // window)).astype(int)
    hann = np.hanning(window)
    power = np.zeros(window // 2 + 1)
    for position in positions:
        spectrum = np.fft.rfft(signal[position:position + window] * hann)
        power += np.abs(spectrum) ** 2
    power /= len(positions)
    freqs = np.fft.rfftfreq(window, 1 / SAMPLE_RATE)
    return freqs, power, 10 * np.log10(np.maximum(power, 1e-20))


def bandwidth(signal, window=8192):
    if len(signal) < window:
        return {"bandwidthHz": None, "rolloff99Hz": None}
    freqs, power, level = average_spectrum(signal, window)

    band_width = 250
    centers = np.arange(1000, SAMPLE_RATE / 2, band_width)
    band_levels = np.array([np.median(level[(freqs >= c) & (freqs < c + band_width)]) for c in centers])
    floor = band_levels.min()
    peak = band_levels.max()
    present = (band_levels > floor + 20) & (band_levels > peak - 70)
    bandwidth_hz = float(centers[present].max() + band_width) if present.any() else 0.0

    cumulative = np.cumsum(power) / power.sum()
    rolloff = float(freqs[np.searchsorted(cumulative, 0.99)])
    return {"bandwidthHz": round(bandwidth_hz), "rolloff99Hz": round(rolloff)}


def clicks(signal, minutes, ratio=30, frame_ms=20):
    impulses = np.abs(np.diff(signal, n=2))
    frame = int(frame_ms / 1000 * SAMPLE_RATE)
    count = len(impulses) // frame
    if count == 0:
        return {"clicksPerMinute": None}
    frames = impulses[:count * frame].reshape(count, frame)
    scale = np.maximum(np.median(frames, axis=1), 1e-6)
    audible = frame_rms_db(signal[:count * frame], frame, frame) > -60
    candidates = np.flatnonzero((frames > ratio * scale[:, None]) & audible[:, None])
    if len(candidates) == 0:
        return {"clicksPerMinute": 0.0}
    merged = 1 + int(np.sum(np.diff(candidates) > 0.002 * SAMPLE_RATE))
    return {"clicksPerMinute": round(merged / minutes, 2)}


def analyze_signal(path):
    signal, channels = decode(path)
    minutes = max(len(signal) / SAMPLE_RATE / 60, 1 / 60)
    metrics = {}
    metrics.update(clipping(channels, minutes))
    del channels
    metrics.update(noise_floor(signal))
    metrics.update(bandwidth(signal))
    metrics.update(clicks(signal, minutes))
    return metrics


def excerpt_offsets(duration, excerpt_seconds, count):
    if duration <= excerpt_seconds:
        return [0]
    fractions = np.linspace(0.25, 0.75, num=count) if count > 1 else [0.4]
    return [max(0, min(int(duration * fraction), int(duration - excerpt_seconds))) for fraction in fractions]


def demucs_binary():
    local = Path(sys.executable).parent / "demucs"
    return str(local) if local.exists() else shutil.which("demucs")


def separate(path, start, excerpt_seconds, work_dir, model):
    excerpt = work_dir / "excerpt.wav"
    run(["ffmpeg", "-y", "-nostats", "-hide_banner", "-v", "error", "-ss", str(start), "-i", str(path),
         "-t", str(excerpt_seconds), "-ac", "2", "-ar", str(SAMPLE_RATE), str(excerpt)])
    run([demucs_binary(), "--two-stems=vocals", "-d", "cpu", "-n", model, "-o", str(work_dir), str(excerpt)])
    stems = work_dir / model / "excerpt"
    return stems / "vocals.wav", stems / "no_vocals.wav"


def voice_profile(vocals_path):
    signal, _ = decode(vocals_path)
    levels = frame_rms_db(signal, frame=SAMPLE_RATE, hop=SAMPLE_RATE)
    if len(levels) == 0:
        return 0.0, None
    activity = float(np.mean(levels > -45))
    voiced = signal[np.repeat(levels > -45, SAMPLE_RATE)[:len(signal)]] if activity > 0 else signal[:0]
    if len(voiced) < 8192:
        return round(activity, 3), None
    freqs, _, level = average_spectrum(voiced)
    low = np.mean(level[(freqs >= 200) & (freqs < 600)])
    high = np.mean(level[(freqs >= 1500) & (freqs < 4000)])
    return round(activity, 3), float(low - high)


def balance(path, duration, excerpt_seconds, excerpt_count, model):
    balances = []
    activities = []
    tilts = []
    for start in excerpt_offsets(duration, excerpt_seconds, excerpt_count):
        with tempfile.TemporaryDirectory(prefix="simply-awake-demucs-") as tmp:
            vocals, music = separate(path, start, excerpt_seconds, Path(tmp), model)
            voice_lufs = loudness(vocals)["integratedLufs"]
            music_lufs = loudness(music)["integratedLufs"]
            activity, tilt = voice_profile(vocals)
            activities.append(activity)
            if tilt is not None:
                tilts.append(tilt)
            if voice_lufs is not None and music_lufs is not None and voice_lufs > -60:
                balances.append(voice_lufs - music_lufs)
    activity = round(float(np.mean(activities)), 3) if activities else 0.0
    voiced = activity >= 0.1
    return {
        "voiceActivityRatio": activity,
        "voiceMusicBalanceDb": round(float(np.median(balances)), 1) if balances and voiced else None,
        "voiceTiltDb": round(float(np.median(tilts)), 1) if tilts and voiced else None,
    }


def grade(value, rule):
    if value is None:
        return None
    if rule["direction"] == "higher":
        return "GOOD" if value >= rule["good"] else "FAIR" if value >= rule["fair"] else "POOR"
    return "GOOD" if value <= rule["good"] else "FAIR" if value <= rule["fair"] else "POOR"


def derive_levels(tracks, thresholds):
    lufs_values = [t["integratedLufs"] for t in tracks.values() if t.get("integratedLufs") is not None]
    median_lufs = float(np.median(lufs_values)) if lufs_values else None
    for track in tracks.values():
        if median_lufs is not None and track.get("integratedLufs") is not None:
            track["loudnessDeltaLu"] = round(abs(track["integratedLufs"] - median_lufs), 1)
        if track.get("truePeakDbfs") is not None and track.get("rmsDb") is not None:
            track["crestDb"] = round(track["truePeakDbfs"] - track["rmsDb"], 1)
        grades = {name: grade(track.get(name), rule) for name, rule in thresholds.items()}
        grades = {name: value for name, value in grades.items() if value is not None}
        track["issues"] = sorted(name for name, value in grades.items() if value != "GOOD")
        track["level"] = max(grades.values(), key=LEVEL_RANK.get) if grades else None
    return median_lufs


def write_csv(tracks, path):
    columns = ["id", "name", "level", "issues", "voiceMusicBalanceDb", "voiceTiltDb", "voiceActivityRatio", "crestDb",
               "integratedLufs", "loudnessDeltaLu", "loudnessRangeLu", "truePeakDbfs", "noiseFloorDb", "rmsDb", "bandwidthHz",
               "rolloff99Hz", "clippedRunsPerMinute", "clippingRatio", "clicksPerMinute", "bitrateKbps",
               "sampleRateHz", "channels", "durationSeconds"]
    ordered = sorted(tracks.values(), key=lambda t: (LEVEL_RANK.get(t.get("level"), 3), t.get("name", "")))
    with open(path, "w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        for track in ordered:
            row = dict(track)
            row["issues"] = " ".join(track.get("issues", []))
            writer.writerow(row)


def load_existing(path):
    if path.exists():
        return json.loads(path.read_text()).get("tracks", {})
    return {}


def save(tracks, thresholds, median_lufs, out_dir):
    payload = {
        "generatedAt": int(time.time()),
        "catalogMedianLufs": median_lufs,
        "thresholds": thresholds,
        "tracks": dict(sorted(tracks.items())),
    }
    (out_dir / "audio_quality.json").write_text(json.dumps(payload, indent=2, ensure_ascii=False))
    write_csv(tracks, out_dir / "audio_quality.csv")


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", default=os.environ.get("SIMPLY_AWAKE_BASE_URL"),
                        help="server root (same value as baseServerUrl in local.properties); env SIMPLY_AWAKE_BASE_URL")
    parser.add_argument("--cache-dir", type=Path, default=Path(__file__).parent / "cache")
    parser.add_argument("--out-dir", type=Path, default=Path(__file__).parent / "out")
    parser.add_argument("--filter", help="regex on track id or name")
    parser.add_argument("--limit", type=int)
    parser.add_argument("--force", action="store_true", help="recompute tracks already in audio_quality.json")
    parser.add_argument("--no-remote-check", action="store_true",
                        help="skip the HEAD request comparing the remote file size with the analysed one")
    parser.add_argument("--jobs", type=int, default=4)
    parser.add_argument("--thresholds", type=Path, help="json overriding DEFAULT_THRESHOLDS")
    parser.add_argument("--balance", action="store_true", help="run demucs voice/music balance (slow)")
    parser.add_argument("--excerpt-seconds", type=int, default=60)
    parser.add_argument("--excerpts", type=int, default=3, help="number of excerpts per track for --balance")
    parser.add_argument("--demucs-model", default="htdemucs")
    parser.add_argument("--list", action="store_true", help="print the catalog and exit")
    return parser.parse_args()


def main():
    args = parse_args()
    if not args.base_url:
        sys.exit("missing --base-url (or SIMPLY_AWAKE_BASE_URL)")
    for tool in ("ffmpeg", "ffprobe"):
        if shutil.which(tool) is None:
            sys.exit(f"{tool} not found: brew install ffmpeg")
    if args.balance and demucs_binary() is None:
        sys.exit("demucs not found: ./setup.sh --balance, then run with .venv-balance/bin/python")

    args.cache_dir.mkdir(parents=True, exist_ok=True)
    args.out_dir.mkdir(parents=True, exist_ok=True)
    thresholds = dict(DEFAULT_THRESHOLDS)
    if args.thresholds:
        thresholds.update(json.loads(args.thresholds.read_text()))

    catalog = fetch_catalog(args.base_url, args.out_dir)
    if args.filter:
        pattern = re.compile(args.filter, re.IGNORECASE)
        catalog = [t for t in catalog if pattern.search(t["id"]) or pattern.search(t.get("name", ""))]
    if args.limit:
        catalog = catalog[:args.limit]
    if args.list:
        for track in catalog:
            print(f'{track["id"]}\t{track.get("duration")}\t{track.get("name")}')
        return
    log(f"{len(catalog)} tracks selected")

    tracks = load_existing(args.out_dir / "audio_quality.json")

    def stage1(entry):
        track_id = entry["id"]
        existing = tracks.get(track_id)
        size = None if args.no_remote_check else remote_size(args.base_url, track_id)
        if existing and not args.force and "bandwidthHz" in existing:
            cached = args.cache_dir / f"{track_id}.mp3"
            if existing.get("fileBytes") is None and cached.exists():
                existing["fileBytes"] = cached.stat().st_size
            if size is None or size == existing.get("fileBytes"):
                return track_id, existing
            log(f"remote file changed {track_id} {entry.get('name')}: full reanalysis")
            existing = None
        path = download(args.base_url, track_id, args.cache_dir, size)
        log(f"analyze {track_id} {entry.get('name')}")
        metrics = {"id": track_id, "name": entry.get("name"), "tagString": entry.get("tagString")}
        metrics.update(probe(path))
        metrics.update(loudness(path))
        metrics.update(analyze_signal(path))
        if existing:
            for key in ("voiceMusicBalanceDb", "voiceActivityRatio", "voiceTiltDb"):
                if key in existing:
                    metrics[key] = existing[key]
        return track_id, metrics

    with ThreadPoolExecutor(max_workers=args.jobs) as pool:
        for track_id, metrics in pool.map(stage1, catalog):
            tracks[track_id] = metrics
            median = derive_levels(tracks, thresholds)
            save(tracks, thresholds, median, args.out_dir)

    if args.balance:
        for entry in catalog:
            track = tracks[entry["id"]]
            if not args.force and track.get("voiceActivityRatio") is not None:
                continue
            path = download(args.base_url, entry["id"], args.cache_dir)
            log(f"balance {entry['id']} {entry.get('name')}")
            track.update(balance(path, track["durationSeconds"], args.excerpt_seconds, args.excerpts,
                                 args.demucs_model))
            median = derive_levels(tracks, thresholds)
            save(tracks, thresholds, median, args.out_dir)

    median = derive_levels(tracks, thresholds)
    save(tracks, thresholds, median, args.out_dir)
    counts = {}
    for track in tracks.values():
        counts[track.get("level")] = counts.get(track.get("level"), 0) + 1
    log(f"done: {counts} -> {args.out_dir / 'audio_quality.csv'}")


if __name__ == "__main__":
    main()
