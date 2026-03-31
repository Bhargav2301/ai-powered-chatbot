# ============================================================
# actions/actions.py — Custom Action Server
# ============================================================
# DATA SOURCES:
#   Movies — Local SQLite (data/movies.db) from Kaggle TMDB CSV
#   Music  — Last.fm Live API
#
# BEFORE RUNNING:
#   1. python scripts/prepare_movies_db.py
#   2. Add LASTFM_API_KEY to .env
#   3. rasa run actions
# ============================================================

import os
import sqlite3
import logging
import requests
from dotenv import load_dotenv
from typing import Any, Text, Dict, List

from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import SlotSet

load_dotenv()
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")
LASTFM_BASE    = "https://ws.audioscrobbler.com/2.0"

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH  = os.path.join(BASE_DIR, "data", "movies.db")

logger = logging.getLogger(__name__)

# ── Genre aliases → TMDB genre names stored in SQLite ────────
# The SQLite genres column contains lowercase values like:
# "action, thriller, crime" — we map user-spoken words to those.
GENRE_ALIASES = {
    "sci-fi": "science fiction",
    "scifi": "science fiction",
    "science-fiction": "science fiction",
    "rom-com": "romance",
    "romcom": "romance",
    "romantic": "romance",
    "funny": "comedy",
    "superhero": "action",
    "zombie": "horror",
    "animated": "animation",
    "cartoon": "animation",
    "anime": "animation",
    "war film": "war",
    "historic": "history",
    "historical": "history",
    "ghost": "horror",
    "vampire": "horror",
    "slasher": "horror",
    "supernatural": "horror",
    "found footage": "horror",
    "spy": "thriller",
    "heist": "crime",
    "caper": "crime",
    "noir": "crime",
    "psychological": "thriller",
    "martial arts": "action",
    "disaster": "action",
    "period": "history",
    "coming of age": "drama",
    "dark comedy": "comedy",
    "mockumentary": "comedy",
    "chick flick": "romance",
    "feel good": "comedy",
    "tearjerker": "drama",
    "mind-bending": "science fiction",
    "space": "science fiction",
    "time travel": "science fiction",
    "dystopian": "science fiction",
    "post-apocalyptic": "science fiction",
    "fairy tale": "fantasy",
    "sword and sorcery": "fantasy",
    "epic": "adventure",
    "road movie": "drama",
    "biopic": "history",
    "true story": "history",
    "nature": "documentary",
    "true crime": "crime",
}


def resolve_genre(raw: str) -> str:
    """
    Normalises a spoken genre to a value likely found in the genres column.
    E.g. "zombie" -> "horror", "sci-fi" -> "science fiction"
    """
    return GENRE_ALIASES.get(raw.lower(), raw.lower())


# ══════════════════════════════════════════════════════════════
# DATABASE HELPERS
# ══════════════════════════════════════════════════════════════

def get_db() -> sqlite3.Connection | None:
    """Opens movies.db with row-by-name access. Returns None if missing."""
    if not os.path.exists(DB_PATH):
        logger.error("movies.db missing at %s — run prepare_movies_db.py", DB_PATH)
        return None
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def query_movies(sql: str, params: tuple = ()) -> list:
    """
    Runs a SQL query and returns results as a list of dicts.
    Always returns [] on any failure — never crashes the action server.
    """
    conn = get_db()
    if not conn:
        return []
    try:
        return [dict(r) for r in conn.execute(sql, params).fetchall()]
    except sqlite3.Error as e:
        logger.error("SQLite error: %s | params=%s | err=%s", sql[:80], params, e)
        return []
    finally:
        conn.close()


# ══════════════════════════════════════════════════════════════
# LAST.FM HELPERS
# ══════════════════════════════════════════════════════════════

def lastfm_get(method: str, params: dict) -> dict | None:
    """Makes a GET request to Last.fm. Returns None on any failure."""
    params.update({"method": method, "api_key": LASTFM_API_KEY, "format": "json"})
    try:
        r = requests.get(LASTFM_BASE, params=params, timeout=5)
        r.raise_for_status()
        data = r.json()
        if "error" in data:
            logger.warning("Last.fm error %s: %s", data["error"], data.get("message"))
            return None
        return data
    except requests.exceptions.Timeout:
        logger.error("Last.fm timeout")
        return None
    except requests.exceptions.RequestException as e:
        logger.error("Last.fm error: %s", e)
        return None


def artist_name(track: dict) -> str:
    """Extracts artist name — Last.fm returns dict or plain string."""
    a = track.get("artist", {})
    return a.get("name", "") if isinstance(a, dict) else str(a)


def strip_lastfm_html(text: str, max_len: int = 350) -> str:
    """Removes Last.fm's HTML link suffixes and truncates."""
    clean = text.split("<a href")[0].strip()
    return (clean[:max_len] + "...") if len(clean) > max_len else clean


# ══════════════════════════════════════════════════════════════
# ACTION 1 — Recommend Movie
# ══════════════════════════════════════════════════════════════
class ActionRecommendMovie(Action):
    """
    Recommends 3 movies from SQLite, filtered by genre if provided.

    FIX: Reads genre from the latest 'genre' ENTITY first (freshest value
    in the message), then falls back to the preferred_genre SLOT.
    Both paths use resolve_genre() to map aliases like "zombie" -> "horror".
    """

    def name(self) -> Text:
        return "action_recommend_movie"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        # Read genre from the latest entity extracted from THIS message first,
        # then fall back to the slot set from a previous turn.
        raw_genre = (
            next(tracker.get_latest_entity_values("genre"), None)
            or tracker.get_slot("preferred_genre")
        )

        if raw_genre:
            genre = resolve_genre(raw_genre)
            rows = query_movies(
                """
                SELECT title, vote_average, release_date, genres
                FROM movies
                WHERE genres LIKE ?
                AND (adult = 'False' OR adult = '0' OR adult IS NULL)
                ORDER BY popularity DESC
                LIMIT 3
                """,
                (f"%{genre}%",),
            )
            # If the specific genre returns nothing, fall back to popular
            if not rows:
                logger.info("No results for genre '%s', falling back to popular", genre)
                raw_genre = None

        if not raw_genre:
            genre = None
            rows = query_movies(
                """
                SELECT title, vote_average, release_date, genres
                FROM movies
                WHERE (adult = 'False' OR adult = '0' OR adult IS NULL)
                ORDER BY popularity DESC
                LIMIT 3
                """
            )

        if not rows:
            dispatcher.utter_message(
                text="I couldn't find movies matching that right now. "
                     "Try genres like 'thriller', 'comedy', 'horror', or 'sci-fi'! 🎬"
            )
            return []

        first_movie = rows[0]["title"]
        genre_text  = f"{raw_genre} " if raw_genre else ""
        movie_list  = "\n".join(
            f"  🎬 {r['title']} ({(r.get('release_date') or '')[:4]}) ⭐ {r.get('vote_average', 'N/A')}"
            for r in rows
        )

        dispatcher.utter_message(
            text=f"Here are some great {genre_text}movies:\n{movie_list}\n\n"
                 f"Want details on any of these?"
        )
        # Update both slots so context is preserved for follow-ups
        events = [SlotSet("current_movie", first_movie)]
        if raw_genre:
            events.append(SlotSet("preferred_genre", raw_genre))
        return events


# ══════════════════════════════════════════════════════════════
# ACTION 2 — Get Movie Details
# ══════════════════════════════════════════════════════════════
class ActionGetMovieDetails(Action):
    """
    Returns overview, rating, runtime, and tagline for a movie.
    Falls back to current_movie slot for context-aware follow-ups.
    """

    def name(self) -> Text:
        return "action_get_movie_details"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        movie_title = (
            next(tracker.get_latest_entity_values("movie_title"), None)
            or tracker.get_slot("current_movie")
        )

        if not movie_title:
            dispatcher.utter_message(text="Which movie would you like details about? 🎬")
            return []

        rows = query_movies(
            """
            SELECT title, overview, vote_average, release_date, runtime, tagline, genres
            FROM movies
            WHERE title LIKE ?
            AND (adult = 'False' OR adult = '0' OR adult IS NULL)
            ORDER BY popularity DESC LIMIT 1
            """,
            (f"%{movie_title}%",),
        )

        if not rows:
            dispatcher.utter_message(
                text=f"I couldn't find '{movie_title}' — check the spelling?"
            )
            return []

        m       = rows[0]
        title   = m["title"]
        year    = (m.get("release_date") or "")[:4]
        rating  = m.get("vote_average", "N/A")
        runtime = m.get("runtime", 0)
        tagline = m.get("tagline", "")
        genres  = m.get("genres", "")
        overview = m.get("overview", "No description available.")
        if len(overview) > 300:
            overview = overview[:300] + "..."

        msg = f"🎬 {title} ({year})\n"
        if tagline:
            msg += f'"{tagline}"\n'
        msg += f"⭐ {rating}/10"
        if runtime:
            msg += f" · {runtime} min"
        if genres:
            msg += f" · {genres.title()}"
        msg += f"\n\n{overview}\n\nWant to know who made it or the cast?"

        dispatcher.utter_message(text=msg)
        return [SlotSet("current_movie", title)]


# ══════════════════════════════════════════════════════════════
# ACTION 3 — Get Movie Director
# ══════════════════════════════════════════════════════════════
class ActionGetMovieDirector(Action):
    """
    Returns production company info (director not in Kaggle CSV).
    Transparently tells the user what data is and isn't available.
    """

    def name(self) -> Text:
        return "action_get_movie_director"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        movie_title = (
            next(tracker.get_latest_entity_values("movie_title"), None)
            or tracker.get_slot("current_movie")
        )

        if not movie_title:
            dispatcher.utter_message(text="Which movie's director are you looking for?")
            return []

        rows = query_movies(
            """
            SELECT title, production_companies, release_date
            FROM movies WHERE title LIKE ?
            AND (adult = 'False' OR adult = '0' OR adult IS NULL)
            ORDER BY popularity DESC LIMIT 1
            """,
            (f"%{movie_title}%",),
        )

        if not rows:
            dispatcher.utter_message(text=f"I couldn't find '{movie_title}'.")
            return []

        m = rows[0]
        title     = m["title"]
        year      = (m.get("release_date") or "")[:4]
        companies = m.get("production_companies", "") or "Unknown"

        dispatcher.utter_message(
            text=f"🎬 My dataset doesn't include director credits for {title} ({year}), "
                 f"but it was produced by: {companies}.\n\n"
                 f"For the director, search: https://www.imdb.com/find?q={title.replace(' ', '+')}"
        )
        return [SlotSet("current_movie", title)]


# ══════════════════════════════════════════════════════════════
# ACTION 4 — Get Movie Cast
# ══════════════════════════════════════════════════════════════
class ActionGetMovieCast(Action):
    """Cast data not in Kaggle CSV — redirects user to IMDb."""

    def name(self) -> Text:
        return "action_get_movie_cast"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        movie_title = (
            next(tracker.get_latest_entity_values("movie_title"), None)
            or tracker.get_slot("current_movie")
        )

        if not movie_title:
            dispatcher.utter_message(text="Which movie's cast are you curious about?")
            return []

        rows = query_movies(
            "SELECT title, release_date FROM movies WHERE title LIKE ? "
            "AND (adult = 'False' OR adult = '0' OR adult IS NULL) "
            "ORDER BY popularity DESC LIMIT 1",
            (f"%{movie_title}%",),
        )

        if not rows:
            dispatcher.utter_message(text=f"I couldn't find '{movie_title}'.")
            return []

        title = rows[0]["title"]
        year  = (rows[0].get("release_date") or "")[:4]
        imdb  = f"https://www.imdb.com/find?q={title.replace(' ', '+')}"

        dispatcher.utter_message(
            text=f"🎬 Cast data isn't in my local dataset for {title} ({year}).\n"
                 f"Full cast on IMDb: {imdb}\n\n"
                 f"I can give you the overview, rating, or genre instead!"
        )
        return [SlotSet("current_movie", title)]


# ══════════════════════════════════════════════════════════════
# ACTION 5 — Get Now Playing
# ══════════════════════════════════════════════════════════════
class ActionGetNowPlaying(Action):
    """Returns recent releases from SQLite (approximate — dataset has a cutoff)."""

    def name(self) -> Text:
        return "action_get_now_playing"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        rows = query_movies(
            """
            SELECT title, release_date, vote_average
            FROM movies
            WHERE release_date >= date('now', '-18 months')
            AND (adult = 'False' OR adult = '0' OR adult IS NULL)
            ORDER BY popularity DESC LIMIT 4
            """
        )

        if not rows:
            rows = query_movies(
                """
                SELECT title, release_date, vote_average FROM movies
                WHERE (adult = 'False' OR adult = '0' OR adult IS NULL)
                ORDER BY release_date DESC, popularity DESC LIMIT 4
                """
            )

        if not rows:
            dispatcher.utter_message(text="Couldn't load recent movies right now. Try again! 🎬")
            return []

        first_movie = rows[0]["title"]
        movie_list  = "\n".join(
            f"  🎬 {r['title']} ({(r.get('release_date') or '')[:4]}) ⭐ {r.get('vote_average','N/A')}"
            for r in rows
        )

        dispatcher.utter_message(
            text=f"Recent popular releases:\n{movie_list}\n\n"
                 f"*(My dataset has a cutoff date — check your cinema for today's listings!)*\n\n"
                 f"Want details on any of these?"
        )
        return [SlotSet("current_movie", first_movie)]


# ══════════════════════════════════════════════════════════════
# ACTION 6 — Recommend Song
# ══════════════════════════════════════════════════════════════
class ActionRecommendSong(Action):
    """Recommends tracks via Last.fm — by genre tag or global charts."""

    def name(self) -> Text:
        return "action_recommend_song"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        raw_genre = (
            next(tracker.get_latest_entity_values("genre"), None)
            or tracker.get_slot("preferred_genre")
        )

        if raw_genre:
            data   = lastfm_get("tag.getTopTracks", {"tag": raw_genre.lower(), "limit": 5})
            tracks = (data or {}).get("tracks", {}).get("track", [])
        else:
            data   = lastfm_get("chart.getTopTracks", {"limit": 5})
            tracks = (data or {}).get("tracks", {}).get("track", [])

        if not tracks:
            dispatcher.utter_message(
                text="Couldn't find music right now. "
                     "Try a genre like 'pop', 'jazz', or 'rock'! 🎵"
            )
            return []

        tracks      = tracks[:4]
        first_art   = artist_name(tracks[0])
        genre_text  = f"{raw_genre} " if raw_genre else ""
        track_list  = "\n".join(
            f"  🎵 {t['name']} — {artist_name(t)}" for t in tracks
        )

        dispatcher.utter_message(
            text=f"Here are some great {genre_text}tracks:\n{track_list}\n\n"
                 f"Want to know more about any of these artists?"
        )
        events = [SlotSet("current_artist", first_art)]
        if raw_genre:
            events.append(SlotSet("preferred_genre", raw_genre))
        return events


# ══════════════════════════════════════════════════════════════
# ACTION 7 — Get Artist Info
# ══════════════════════════════════════════════════════════════
class ActionGetArtistInfo(Action):
    """Returns artist bio, listeners, and playcount from Last.fm."""

    def name(self) -> Text:
        return "action_get_artist_info"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        name = (
            next(tracker.get_latest_entity_values("artist_name"), None)
            or tracker.get_slot("current_artist")
        )

        if not name:
            dispatcher.utter_message(text="Which artist would you like to know about? 🎵")
            return []

        data = lastfm_get("artist.getInfo", {"artist": name, "autocorrect": 1})

        if not data or "artist" not in data:
            dispatcher.utter_message(
                text=f"Couldn't find info on '{name}'. Check the spelling?"
            )
            return []

        a         = data["artist"]
        name      = a.get("name", name)
        listeners = int(a.get("stats", {}).get("listeners", 0))
        playcount = int(a.get("stats", {}).get("playcount", 0))
        bio       = strip_lastfm_html(a.get("bio", {}).get("summary", "No bio available."))

        dispatcher.utter_message(
            text=f"🎵 {name}\n"
                 f"👥 {listeners:,} listeners · ▶️ {playcount:,} plays\n\n"
                 f"{bio}\n\n"
                 f"Want to hear their top tracks?"
        )
        return [SlotSet("current_artist", name)]


# ══════════════════════════════════════════════════════════════
# ACTION 8 — Get Song Details
# ══════════════════════════════════════════════════════════════
class ActionGetSongDetails(Action):
    """Returns track stats and wiki summary from Last.fm."""

    def name(self) -> Text:
        return "action_get_song_details"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        song = next(tracker.get_latest_entity_values("song_title"), None)
        art  = (
            next(tracker.get_latest_entity_values("artist_name"), None)
            or tracker.get_slot("current_artist")
        )

        if not song:
            dispatcher.utter_message(text="Which song would you like to know about? 🎵")
            return []

        params = {"track": song, "autocorrect": 1}
        if art:
            params["artist"] = art

        data = lastfm_get("track.getInfo", params)

        if not data or "track" not in data:
            dispatcher.utter_message(
                text=f"Couldn't find details for '{song}'. Try including the artist name!"
            )
            return []

        t         = data["track"]
        name      = t.get("name", song)
        art_name  = t.get("artist", {}).get("name", "Unknown Artist")
        listeners = int(t.get("listeners", 0))
        playcount = int(t.get("playcount", 0))
        wiki_raw  = t.get("wiki", {}).get("summary", "")
        wiki      = strip_lastfm_html(wiki_raw, 300) if wiki_raw else ""

        msg = (
            f"🎵 {name} by {art_name}\n"
            f"👥 {listeners:,} listeners · ▶️ {playcount:,} plays\n"
        )
        if wiki:
            msg += f"\n{wiki}"
        msg += "\n\nWant more from this artist?"

        dispatcher.utter_message(text=msg)
        return [SlotSet("current_artist", art_name)]


# ══════════════════════════════════════════════════════════════
# ACTION 9 — Get Top Songs
# ══════════════════════════════════════════════════════════════
class ActionGetTopSongs(Action):
    """Returns global top 5 from Last.fm live charts."""

    def name(self) -> Text:
        return "action_get_top_songs"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        data   = lastfm_get("chart.getTopTracks", {"limit": 5})
        tracks = (data or {}).get("tracks", {}).get("track", [])

        if not tracks:
            dispatcher.utter_message(text="Couldn't load charts right now. Try again shortly! 🎵")
            return []

        first_art  = artist_name(tracks[0])
        track_list = "\n".join(
            f"  {i+1}. 🎵 {t['name']} — {artist_name(t)}"
            for i, t in enumerate(tracks[:5])
        )

        dispatcher.utter_message(
            text=f"🔥 Current Top Songs on Last.fm:\n{track_list}\n\n"
                 f"Want details on any of these tracks or artists?"
        )
        return [SlotSet("current_artist", first_art)]
