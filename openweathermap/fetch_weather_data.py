"""
This python script gets the current weather from the openweather API.
"""
import csv
import json
import os
from datetime import datetime
from typing import List, Dict
from urllib.error import HTTPError

import requests
from dotenv import load_dotenv
from requests import Response

from utils.project_patterns import get_full_class_name, find_project_root

# Get project root for the environment variables
project_root = find_project_root()
if project_root is None:
    raise ValueError("Could not determine project root")

# Get configuration information
load_dotenv()
api_key: str | None = os.getenv("API_KEY")
city_file: str = os.path.join(project_root, os.getenv("CITY_FILE", "default_cities.csv"))
output_files: str = os.path.join(project_root + os.getenv("OUTPUT_FILES", ""))
owm_geo_url: str | None = os.getenv("OWM_GEO_URL")
owm_cur_weather_url: str | None = os.getenv("OWM_CUR_WEATHER_URL")
own_forecast_url: str | None = os.getenv("OWM_FRC_WEATHER_URL")
language: str | None = os.getenv("LANGUAGE")
units_of_measure: str | None = os.getenv("UNITS_OF_MEASURE")

# Get current datetime for the filename or content
current_datetime = datetime.now()
formatted_datetime = current_datetime.strftime("%Y%m%d%H%M%S")


def read_locations_file(file_path: str, encoding: str = 'utf-8', delimiter: str = ',') \
        -> List[Dict[str, str]]:
    """
    Read a file containing city, state, country information.

    Args:
        file_path: Path to the file
        encoding: File encoding (default: utf-8)
        delimiter: Column separator (default: comma)

    Returns:
        List of dictionaries with city, state, and country information
    """
    locations = []

    try:
        with open(file_path, 'r', encoding=encoding) as file:
            # Check if file is CSV or plain text
            if file_path.endswith('.csv'):
                reader = csv.DictReader(file)
                for row in reader:
                    locations.append({
                        'city': row.get('city', '').strip(),
                        'state': row.get('state', '').strip(),
                        'country': row.get('country', '').strip()
                    })
            else:
                # Handle plain text file with assumed format
                for line in file:
                    parts = line.strip().split(delimiter)
                    if len(parts) >= 3:
                        locations.append({
                            'city': parts[0].strip(),
                            'state': parts[1].strip(),
                            'country': parts[2].strip()
                        })
                    elif len(parts) == 2:
                        # Handle case where state might be missing
                        locations.append({
                            'city': parts[0].strip(),
                            'state': '',
                            'country': parts[1].strip()
                        })

        print(f"Successfully read {len(locations)} locations from {file_path}")
    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.")
        locations = []

    return locations


def get_lan_lon(city_name: str, state_code: str, country_code: str, limit: int = 1) \
        -> tuple[float, float]:
    """

    :param city_name:
    :param state_code:
    :param country_code:
    :param limit:
    :return:
    """
    lat = float('inf')
    lon = float('inf')

    try:
        resp: Response = requests.get(
            f"{owm_geo_url}{city_name},{state_code},{country_code}&limit={limit}&appid={api_key}",
            timeout=5)
        print(f"get_lan_lon: {resp.status_code = }")
        print(f"{resp.json() = }")
        print(f"{type(resp.json()) = }")
        if resp.status_code == 200 and resp.json():
            # location_data = Location.from_dict(resp.json()[0])
            print(
                f"{resp.json()[0].get('name') = }, {resp.json()[0].get('lat') = },"
                f"{resp.json()[0].get('lon') = }")
            lat = float(resp.json()[0].get('lat'))
            lon = float(resp.json()[0].get('lon'))
    except HTTPError as e:
        print(f"{get_full_class_name(e)}: {e.read().decode()}")
        # location_data = None
    except (IndexError, ValueError, TypeError, AttributeError, ImportError, NameError) as e:
        print(f"{get_full_class_name(e)}: {e.args}")
        # location_data = None

    return lat, lon


def get_current_weather(lat: float, lon: float, location: str) -> None:
    """
    :param lat:      The latitude to get the current weather from.
    :param lon:      The longitude to get the current weather from.
    :param location:  The location to get the current weather from.

    :return:
    """
    try:
        print("\n")
        resp: Response = requests.get(
            f"{owm_cur_weather_url}lat={lat}&lon={lon}&lang={language}&appid={api_key}"
            f"&units={units_of_measure}", timeout=5)
        print(f"get_current_weather: {resp.status_code = }")
        print(f"{resp.json() = }")
        print(f"{type(resp.json()) = }")
        if resp.status_code == 200 and resp.json():
            # current_weather_data = CurrentWeatherData.from_dict(resp.json(), units_of_measure)
            print(f"{resp.json() = }")
            # Save the data to a file
            file_path = f"{output_files}current_weather_{location}_{formatted_datetime}.json"
            with open(file_path, "w", encoding="utf-8") as file:
                file.write(json.dumps(resp.json(), indent=2))
    except HTTPError as e:
        print(f"{get_full_class_name(e)}: {e.read().decode()}")
    except (IndexError, ValueError, TypeError, AttributeError) as e:
        print(f"{get_full_class_name(e)}: {e.args}")


def get_forcast(lat: float, lon: float, location: str) -> None:
    """
    :param lat:      The latitude to get the forecast from.
    :param lon:      The longitude to get the forecast from.
    :param location:  The location to get the forecast from.

    :return:
    """
    try:
        print("\n")
        resp: Response = requests.get(
            f"{own_forecast_url}lat={lat}&lon={lon}&lang={language}&appid={api_key}"
            f"&units={units_of_measure}", timeout=5)
        print(f"get_forcast: {resp.status_code = }")
        print(f"{resp.json() = }")
        print(f"{type(resp.json()) = }")
        if resp.status_code == 200 and resp.json():
            # forecast_data = Forecast.from_dict(resp.json(), units_of_measure)
            print(f"{resp.json() = }")
            # Save the data to a file
            file_path = f"{output_files}forecast_{location}_{formatted_datetime}.json"
            with open(file_path, "w", encoding="utf-8") as file:
                file.write(json.dumps(resp.json(), indent=2))
    except HTTPError as e:
        print(f"{get_full_class_name(e)}: {e.read().decode()}")
    except (IndexError, ValueError, TypeError, AttributeError) as e:
        print(f"{get_full_class_name(e)}: {e.args}")


def main() -> None:
    """

    :return:
    """
    if not api_key:
        print("Error: No API key found. Please check the API_KEY environment variable.")
        return

    # Get locations from input file
    locations: list[dict[str, str]] | None = read_locations_file(f"{city_file}")

    if not locations:
        print("No locations were loaded. Please check the file path and format.")
        return

    # Print first 5 locations as example
    for i, location in enumerate(locations[:5]):
        print(f"{i + 1}. {location['city']}, {location['state']}, {location['country']}")

    # Example of filtering locations
    # us_cities = [loc for loc in locations if loc['country'].lower()
    #        in ('us', 'usa', 'united states')]
    # print(f"\nFound {len(us_cities)} cities")

    selected_location = locations[0]
    print(f"\nGetting the latitude and longitude for city, state, country: "
          f"{selected_location['city']}, {selected_location['state']}, "
          f"{selected_location['country']}")
    lat, lon = get_lan_lon(selected_location['city'], selected_location['state'],
                           selected_location['country'])
    if lat != float('inf') and lon != float('inf'):
        print(f"\n{os.path.basename(__file__)}: "
              f"Fetching current weather data for coordinates: {lat}, {lon}")
        get_current_weather(lat, lon,
                            f"{selected_location['city']}_{selected_location['state']}_"
                            f"{selected_location['country']}")
        get_forcast(lat, lon, f"{selected_location['city']}_{selected_location['state']}_"
                              f"{selected_location['country']}")
    else:
        print("No valid latitude or longitude found.")


if __name__ == '__main__':
    main()
    # main('Toronto', '', country_code='CA')
    # main('Dublin', '', country_code = 'IE')
    # main('London', '', country_code='GB')
    # main('Solon', 'OH', 'US')
    # main('Letlhakane', '', country_code='BW')
