import os
import sqlite3
import requests
from typing import Any, Text, Dict, List
from dotenv import load_dotenv

from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import SlotSet

# Load our API Keys from .env securely
load_dotenv()
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")
LASTFM_BASE_URL = "https://ws.audioscrobbler.com/2.0/"

# Define standard timeout for network resilience (DECISION-010)
HTTP_TIMEOUT = 5

def query_movies_db(query: str, params: tuple = ()) -> List[Dict[str, Any]]:
    """Helper function to execute localized SQLite queries gracefully."""
    db_path = os.path.join("data", "movies.db")
    try:
        with sqlite3.connect(db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute(query, params)
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
    except Exception as e:
        print(f"Database error: {e}")
        return []

# ==============================================================================
# 🎬 MOVIE ACTIONS (SQLite Local DB via Kaggle Dataset)
# ==============================================================================

class ActionRecommendMovie(Action):
    """Recommends top 3 trending movies either universally or filtered by genre."""
    
    def name(self) -> Text:
        return "action_recommend_movie"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        
        # User requested genre mapping via slot if available
        # Note: Depending on NLU pipeline, slots might be named differently
        # We will attempt a generic pull first based on popularity.
        
        # Standard query fetching top 3 most popular
        query = "SELECT title, overview FROM movies ORDER BY popularity DESC LIMIT 3"
        
        try:
            results = query_movies_db(query)
            
            if not results:
                dispatcher.utter_message(text="I couldn't find any solid movie recommendations right now.")
                return []
                
            response = "Here are a few ultra-popular movies right now:\n\n"
            for row in results:
                response += f"🎬 {row['title']}\n"
            
            dispatcher.utter_message(text=response)
            
            # Store the top movie title inside the conversational slot 
            # to allow context-aware followup stories!
            top_movie = results[0]['title']
            return [SlotSet("current_movie", top_movie)]
            
        except Exception as e:
            dispatcher.utter_message(text="Sorry, my movie database seems to be offline.")
            return []

class ActionGetMovieDetails(Action):
    """Fetches the plot overview for the movie currently saved in context."""
    
    def name(self) -> Text:
        return "action_get_movie_details"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        
        current_movie = tracker.get_slot("current_movie")
        
        if not current_movie:
            dispatcher.utter_message(text="I'm not sure which movie you're referring to.")
            return []
            
        query = "SELECT overview FROM movies WHERE title = ? COLLATE NOCASE LIMIT 1"
        try:
            results = query_movies_db(query, (current_movie,))
            if results:
                overview = results[0].get('overview', 'No summary available.')
                dispatcher.utter_message(text=f"Here is what '{current_movie}' is about:\n{overview}")
            else:
                dispatcher.utter_message(text=f"I couldn't find details for '{current_movie}' in my database.")
        except Exception as e:
            dispatcher.utter_message(text="My connection to the movie archive failed. Try again soon!")
            
        return []

class ActionGetMovieDirector(Action):
    """Honest fallback for Director (Not natively contained in our TMDB Kaggle CSV matrix)."""
    
    def name(self) -> Text:
        return "action_get_movie_director"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        current_movie = tracker.get_slot("current_movie")
        if current_movie:
            dispatcher.utter_message(text=f"I don't track director credits locally for '{current_movie}'. I highly recommend checking IMDb for the exact filmography!")
        else:
            dispatcher.utter_message(text="I don't remember which movie we were discussing, but I don't store director credits heavily anyway.")
        return []

class ActionGetMovieCast(Action):
    """Honest fallback for Cast (Not natively contained in our TMDB Kaggle CSV matrix)."""
    
    def name(self) -> Text:
        return "action_get_movie_cast"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        current_movie = tracker.get_slot("current_movie")
        if current_movie:
            dispatcher.utter_message(text=f"I unfortunately don't have the cast lists saved for '{current_movie}'. IMDb has the full cast ensemble!")
        else:
            dispatcher.utter_message(text="I'm not storing cast arrays natively at this moment.")
        return []
        
class ActionGetNowPlaying(Action):
    """Retrieves 3 most recent high-profile movies based on release_date and popularity constraints."""
    
    def name(self) -> Text:
        return "action_get_now_playing"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        # Select high popularity newer movies
        query = "SELECT title, release_date FROM movies ORDER BY release_date DESC, popularity DESC LIMIT 3"
        try:
            results = query_movies_db(query)
            if results:
                msg = "Here are a few recently cataloged releases:\n"
                for row in results:
                    msg += f"🍿 {row['title']} (Released: {row['release_date']})\n"
                dispatcher.utter_message(text=msg)
            else:
                dispatcher.utter_message(text="I couldn't find any recent theaters.")
        except Exception:
            dispatcher.utter_message(text="I can't reach the backend to find new movies right now.")
            
        return []


# ==============================================================================
# 🎵 MUSIC ACTIONS (Live API calls to Last.fm)
# ==============================================================================

class ActionRecommendSong(Action):
    """Calls Last.FM to retrieve trending tracks globally."""
    
    def name(self) -> Text:
        return "action_recommend_song"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        params = {
            "method": "chart.getTopTracks",
            "api_key": LASTFM_API_KEY,
            "format": "json",
            "limit": 3
        }
        
        try:
            # 5-second graceful execution timeout
            response = requests.get(LASTFM_BASE_URL, params=params, timeout=HTTP_TIMEOUT)
            response.raise_for_status()
            data = response.json()
            
            tracks = data.get("tracks", {}).get("track", [])
            if not tracks:
                dispatcher.utter_message(text="I couldn't locate any trending music right now.")
                return []
                
            reply = "Here are the top trending tracks in the world right now:\n\n"
            for t in tracks:
                reply += f"🎧 {t['name']} by {t['artist']['name']}\n"
                
            dispatcher.utter_message(text=reply)
            
            # Map top tracked artist into context mapping for conversation fluidity
            top_artist = tracks[0]['artist']['name']
            return [SlotSet("current_artist", top_artist)]
            
        except requests.exceptions.Timeout:
            dispatcher.utter_message(text="Sorry, the request to Last.fm timed out. Try again!")
            return []
        except requests.exceptions.RequestException:
            dispatcher.utter_message(text="I couldn't fetch trending music right now. Try again later!")
            return []

class ActionGetSongDetails(Action):
    """Stub implementation or generic HTTP routing for track details via Last.fm."""
    
    def name(self) -> Text:
        return "action_get_song_details"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        current_artist = tracker.get_slot("current_artist")
        
        if current_artist:
            dispatcher.utter_message(text=f"I recommend looking up {current_artist} directly on streaming platforms to explore their entire track history!")
        else:
            dispatcher.utter_message(text="I'm not sure which track we are actively reviewing.")
        return []

class ActionGetArtistInfo(Action):
    """Retrieves standard biography and statistics utilizing Last.Fms artist.getinfo."""
    
    def name(self) -> Text:
        return "action_get_artist_info"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        current_artist = tracker.get_slot("current_artist")
        if not current_artist:
            dispatcher.utter_message(text="I'm not exactly sure which artist you mean.")
            return []
            
        params = {
            "method": "artist.getinfo",
            "artist": current_artist,
            "api_key": LASTFM_API_KEY,
            "format": "json"
        }
        
        try:
            response = requests.get(LASTFM_BASE_URL, params=params, timeout=HTTP_TIMEOUT)
            response.raise_for_status()
            data = response.json()
            
            artist_data = data.get("artist")
            if artist_data:
                bio = artist_data.get("bio", {}).get("summary", "No bio available.").split('<a href')[0]
                dispatcher.utter_message(text=f"Here is a quick rundown on {current_artist}:\n\n{bio}")
            else:
                dispatcher.utter_message(text=f"I couldn't find an exact match for {current_artist} on Last.fm.")
                
        except requests.exceptions.Timeout:
            dispatcher.utter_message("Sorry, the last.fm connection timed out!")
        except Exception:
            dispatcher.utter_message("I encountered an issue verifying that artist.")
            
        return [SlotSet("current_artist", current_artist)]

class ActionGetTopSongs(Action):
    """Gets top songs filtered per the currently active artist."""
    
    def name(self) -> Text:
        return "action_get_top_songs"
        
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
            
        current_artist = tracker.get_slot("current_artist")
        if not current_artist:
            dispatcher.utter_message(text="Please specify an artist before searching their top tracks!")
            return []
            
        params = {
            "method": "artist.gettoptracks",
            "artist": current_artist,
            "api_key": LASTFM_API_KEY,
            "format": "json",
            "limit": 3
        }
        
        try:
            response = requests.get(LASTFM_BASE_URL, params=params, timeout=HTTP_TIMEOUT)
            response.raise_for_status()
            data = response.json()
            
            tracks = data.get("toptracks", {}).get("track", [])
            if not tracks:
                dispatcher.utter_message(text=f"I couldn't find tracks for {current_artist}.")
                return []
                
            reply = f"Here are the top tracks for {current_artist}:\n\n"
            for t in tracks:
                reply += f"🎧 {t['name']}\n"
                
            dispatcher.utter_message(text=reply)
            
        except Exception:
            dispatcher.utter_message(text="I failed to retrieve top tracks right now.")
            
        return []
