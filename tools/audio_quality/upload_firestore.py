#!/usr/bin/env python3
"""Upload out/audio_quality.json to Firestore: one document per track in the `audioQuality` collection."""

import argparse
import json
from pathlib import Path

FIELDS = ("level", "issues", "voiceMusicBalanceDb", "voiceTiltDb", "voiceActivityRatio", "crestDb", "integratedLufs",
          "truePeakDbfs", "loudnessDeltaLu", "noiseFloorDb", "bandwidthHz", "clippedRunsPerMinute", "clicksPerMinute",
          "bitrateKbps")


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--credentials", type=Path, required=True,
                        help="service account json (Firebase console > Project settings > Service accounts)")
    parser.add_argument("--input", type=Path, default=Path(__file__).parent / "out" / "audio_quality.json")
    parser.add_argument("--collection", default="audioQuality")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def main():
    args = parse_args()
    payload = json.loads(args.input.read_text())
    tracks = payload["tracks"]
    generated_at = payload["generatedAt"]

    documents = {}
    for track_id, track in tracks.items():
        if track.get("level") is None:
            continue
        document = {name: track[name] for name in FIELDS if track.get(name) is not None}
        document["generatedAt"] = generated_at
        documents[track_id] = document

    if args.dry_run:
        print(json.dumps(documents, indent=2))
        print(f"{len(documents)} documents (dry run)")
        return

    import firebase_admin
    from firebase_admin import credentials, firestore

    firebase_admin.initialize_app(credentials.Certificate(str(args.credentials)))
    db = firestore.client()
    collection = db.collection(args.collection)
    ids = list(documents)
    for start in range(0, len(ids), 400):
        batch = db.batch()
        for track_id in ids[start:start + 400]:
            batch.set(collection.document(track_id), documents[track_id])
        batch.commit()
    print(f"{len(documents)} documents written to {args.collection} ({firebase_admin.get_app().project_id})")


if __name__ == "__main__":
    main()
