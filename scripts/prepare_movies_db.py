# ============================================================
# scripts/prepare_movies_db.py — Kaggle CSV → SQLite Converter
# ============================================================
# RUN THIS to rebuild the database after any filter changes:
#   .\venv\Scripts\activate
#   python scripts/prepare_movies_db.py
#
# INPUT:  data/TMDB_all_movies.csv
# OUTPUT: data/movies.db (SQLite, gitignored)
# ============================================================

import os
import sqlite3
import pandas as pd

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(BASE_DIR, "data", "TMDB_all_movies.csv")
DB_PATH  = os.path.join(BASE_DIR, "data", "movies.db")


def clean_genres(genre_str: str) -> str:
    if not isinstance(genre_str, str) or not genre_str.strip():
        return ""
    return ", ".join(g.strip().lower() for g in genre_str.split(","))


def main():
    print("=" * 60)
    print("  TMDB Kaggle CSV → SQLite Converter")
    print("=" * 60)

    if not os.path.exists(CSV_PATH):
        print(f"\n ERROR: CSV not found at: {CSV_PATH}")
        print("Download from Kaggle, rename to TMDB_all_movies.csv, place in data/")
        return

    print(f"\n Loading CSV (may take 30-60 seconds)...")

    COLUMNS_TO_KEEP = [
        "id", "title", "overview", "genres", "release_date",
        "vote_average", "vote_count", "popularity", "runtime",
        "status", "tagline", "original_language",
        "production_companies", "keywords", "adult",
    ]

    try:
        df = pd.read_csv(
            CSV_PATH,
            usecols=lambda c: c in COLUMNS_TO_KEEP,
            low_memory=False,
        )
    except Exception as e:
        print(f"\n ERROR: Failed to read CSV: {e}")
        return

    print(f"   Loaded {len(df):,} rows from CSV")
    print("\n Filtering...")

    # ── STEP 1: Remove ALL adult content ─────────────────────
    # The adult column may be True/False booleans or 'True'/'False' strings.
    # We convert everything to lowercase string and keep only 'false' / '0'.
    # This is the most important filter — do not remove it.
    adult_col = df["adult"].astype(str).str.strip().str.lower()
    before = len(df)
    df = df[adult_col.isin(["false", "0", "no", ""])].copy()
    print(f"   Removed {before - len(df):,} adult-flagged rows -> {len(df):,} remaining")

    # ── STEP 2: Keep Released movies with meaningful votes ────
    df = df[
        (df["vote_count"].fillna(0) > 50) &
        (df["status"].fillna("") == "Released")
    ].copy()
    print(f"   After vote_count>50 and status=Released: {len(df):,} rows")

    # ── STEP 3: Top 50,000 by popularity ─────────────────────
    df = df.sort_values("popularity", ascending=False).head(50_000)
    print(f"   Kept top 50,000 by popularity: {len(df):,} rows")

    # ── STEP 4: Clean and normalise columns ──────────────────
    # Genres to lowercase comma-separated: "Action, Thriller" -> "action, thriller"
    df["genres"] = df["genres"].fillna("").apply(clean_genres)

    for col in ["overview", "tagline", "production_companies", "keywords"]:
        df[col] = df[col].fillna("")

    df["vote_average"] = pd.to_numeric(df["vote_average"], errors="coerce").fillna(0)
    df["vote_count"]   = pd.to_numeric(df["vote_count"],   errors="coerce").fillna(0).astype(int)
    df["popularity"]   = pd.to_numeric(df["popularity"],   errors="coerce").fillna(0)
    df["runtime"]      = pd.to_numeric(df["runtime"],      errors="coerce").fillna(0).astype(int)

    # ── STEP 5: Write to SQLite ───────────────────────────────
    print(f"\n Writing to SQLite: {DB_PATH}")
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print("   Removed old movies.db")

    conn = sqlite3.connect(DB_PATH)
    try:
        df.to_sql("movies", conn, if_exists="replace", index=False)

        # Indexes make genre/title queries ~100x faster
        conn.execute("CREATE INDEX IF NOT EXISTS idx_title      ON movies(title)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_popularity ON movies(popularity DESC)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_release    ON movies(release_date)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_genres     ON movies(genres)")
        conn.commit()

        count = conn.execute("SELECT COUNT(*) FROM movies").fetchone()[0]
        sample = conn.execute(
            "SELECT title, genres, vote_average FROM movies ORDER BY popularity DESC LIMIT 5"
        ).fetchall()

        print(f"\n Database ready: {count:,} movies")
        print("   Top 5 by popularity (verify these look like mainstream films):")
        for title, genres, rating in sample:
            print(f"     {title[:50]:<50} | {genres[:25]:<25} | {rating}")

    except Exception as e:
        print(f"\n ERROR writing database: {e}")
    finally:
        conn.close()

    print("\n Done! Run: rasa run actions")


if __name__ == "__main__":
    main()
