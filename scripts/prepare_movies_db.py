import kagglehub
import os
import shutil
import sqlite3
import pandas as pd
import glob

# Configuration
DATA_DIR = "data"
CSV_DESTINATION = os.path.join(DATA_DIR, "TMDB_all_movies.csv")
DB_DESTINATION = os.path.join(DATA_DIR, "movies.db")
ROW_LIMIT = 50000

print("==> Initializing Phase 4 Database Setup...")

# 1. Ensure Data Directory exists
if not os.path.exists(DATA_DIR):
    os.makedirs(DATA_DIR)

# 2. Download Kaggle Dataset (if not already cached)
print("\n==> 1. Downloading TMDB Dataset via KaggleHub...")
try:
    path = kagglehub.dataset_download("asaniczka/tmdb-movies-dataset-2023-930k-movies")
    print(f"Kaggle Dataset successfully verified at: {path}")
except Exception as e:
    print(f"❌ Kaggle download failed: {e}")
    print("Please ensure you have authenticated or the package is working.")
    exit(1)

# 3. Locate CSV in the downloaded folder
csv_files = glob.glob(os.path.join(path, "*.csv"))
if not csv_files:
    print("❌ No CSV file discovered in the downloaded dataset.")
    exit(1)

source_csv = csv_files[0]
print(f"\n==> 2. Copying raw CSV to project data directory...")
shutil.copy(source_csv, CSV_DESTINATION)
print(f"File placed at: {CSV_DESTINATION}")

# 4. Load Data & Filter to highest popularity
print("\n==> 3. Processing CSV via Pandas (Sorting & Filtering)...")
try:
    # We read only high priority columns or simply read all and let pandas optimize.
    df = pd.read_csv(CSV_DESTINATION)
    
    # Sort by Popularity descending if available
    if 'popularity' in df.columns:
        df = df.sort_values(by='popularity', ascending=False)
        print("Dataset sorted by Popularity threshold.")
    
    # Prune rows to prevent massive latency (limiting to top 50k)
    df = df.head(ROW_LIMIT)
    print(f"Dataset securely truncated to the top {ROW_LIMIT} rows.")
except Exception as e:
    print(f"❌ Pandas execution failed: {e}")
    exit(1)

# 5. Compile into SQLite Database
print("\n==> 4. Exporting data matrix into SQLite Database...")
try:
    with sqlite3.connect(DB_DESTINATION) as conn:
        df.to_sql('movies', conn, if_exists='replace', index=False)
        
        # Optimize title searches (ignoring case)
        if 'title' in df.columns:
            conn.execute("CREATE INDEX IF NOT EXISTS idx_title ON movies (title COLLATE NOCASE);")
        if 'genres' in df.columns:
            conn.execute("CREATE INDEX IF NOT EXISTS idx_genres ON movies (genres);")
            
    print(f"SQLite Compilation Successful! File placed at: {DB_DESTINATION}")
except Exception as e:
    print(f"❌ SQLite database compilation failed: {e}")
    exit(1)

print("\n🚀 Database Initialization Complete! You are ready to map SQLite queries inside your custom Python endpoints.")
