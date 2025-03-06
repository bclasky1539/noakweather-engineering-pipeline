"""
This python script gets the current weather from the openweather API.
"""
import json
import os
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv

from openweathermap.datasets.current_weather import FactCurrentWeather
from openweathermap.datasets.shared import (WeatherDataDTO, WeatherMapper, DateDimensionFactory,
                                            TimeDimensionFactory)
from openweathermap.datasets.weather import DimCity, DimWeatherCondition, PrecipitationType
from utils.project_patterns import find_project_root, safe_decimal, safe_datetime

# Get project root for the environment variables
project_root = find_project_root()
if project_root is None:
    raise ValueError("Could not determine project root")

# Get configuration information
load_dotenv()
api_key: str | None = os.getenv("API_KEY")
city_file: str = os.path.join(project_root, os.getenv("CITY_FILE", "default_cities.csv"))
output_files: str = os.path.join(project_root + os.getenv("OUTPUT_FILES", ""))
output_bkp_files: str = os.path.join(project_root + os.getenv("OUTPUT_BKP_FILES", ""))
owm_geo_url: str | None = os.getenv("OWM_GEO_URL")
owm_cur_weather_url: str | None = os.getenv("OWM_CUR_WEATHER_URL")
own_forecast_url: str | None = os.getenv("OWM_FRC_WEATHER_URL")
language: str | None = os.getenv("LANGUAGE")
units_of_measure: str | None = os.getenv("UNITS_OF_MEASURE")

# Get current datetime for the filename or content
current_datetime = datetime.now()
formatted_datetime = current_datetime.strftime("%Y%m%d%H%M%S")


# Data Processing Functions
def find_and_load_weather_file() -> tuple[dict, str] | tuple[None, None]:
    """
    Find and load the latest current weather JSON file.

    Returns:
        tuple: (data, filename) or (None, None) if no file found or loading fails
    """
    try:
        cw_json_files = list(Path(f"{output_files}").glob("current_weather_*.json"))
        if not cw_json_files:
            print("No current weather files found")
            return None, None

        filename = cw_json_files[0]
        print(f"{cw_json_files = }, {filename = }")

        # Load the JSON data
        with open(filename, 'r', encoding="utf-8") as file:
            data = json.load(file)

        print(f"Successfully loaded current weather data from {filename}")
        return data, str(filename)
    except (FileNotFoundError, PermissionError, IOError) as e:
        print(f"Error accessing file or directory: {e}")
        return None, None
    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON file: {e}")
        return None, None


def create_city_object(data: dict) -> DimCity | None:
    """
    Create city dimension object from weather data.

    Args:
        data: The weather JSON data

    Returns:
        DimCity object or None if creation fails
    """
    try:
        city_data = {
            'city_id': data.get('id', 0),
            'name': data.get('name', 'Unknown'),
            'country': data.get('sys', {}).get('country', 'Unknown'),
            'timezone': data.get('timezone', 0),
            'latitude': safe_decimal(data.get('coord', {}).get('lat', 0)),
            'longitude': safe_decimal(data.get('coord', {}).get('lon', 0)),
            'state': data.get('sys', {}).get('state', 'Unknown'),
            'population': data.get('city', {}).get('population', 0),
            'sunrise': safe_datetime(data.get('sys', {}).get('sunrise', 0)),
            'sunset': safe_datetime(data.get('sys', {}).get('sunset', 0))
        }

        return DimCity.from_dict(city_data)
    except (ValueError, TypeError) as e:
        print(f"Error processing city data: {e}")
        return None


def create_weather_conditions(data: dict) -> list[DimWeatherCondition]:
    """
    Create weather condition objects from the weather data.

    Args:
        data: The weather JSON data

    Returns:
        List of DimWeatherCondition objects
    """
    weather_conditions = []

    try:
        for weather_item in data.get('weather', []):
            weather_condition_data = {
                'condition_id': weather_item.get('id', 800),
                'condition_main': weather_item.get('main', 'Unknown'),
                'description': weather_item.get('description', 'No description available'),
                'icon_code': weather_item.get('icon', '01d'),
                'is_precipitation': weather_item.get('main', '') in ['Rain', 'Snow', 'Drizzle']
            }
            condition = DimWeatherCondition.from_dict(weather_condition_data)
            weather_conditions.append(condition)

        # If no weather conditions found, create a default one
        if not weather_conditions:
            default_weather_data = {
                'condition_id': 800,  # Clear sky
                'condition_main': 'Unknown',
                'description': 'No description available',
                'icon_code': '01d',
                'is_precipitation': False,
                'is_extreme': False
            }
            weather_conditions.append(DimWeatherCondition.from_dict(default_weather_data))

        print(f"{type(weather_conditions) = }, {len(weather_conditions) = }")
        return weather_conditions
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error processing weather conditions: {e}")
        # Return a default condition rather than failing
        default_condition = DimWeatherCondition.from_dict({
            'condition_id': 800,
            'condition_main': 'Unknown',
            'description': 'Default condition due to error',
            'icon_code': '01d',
            'is_precipitation': False,
            'is_extreme': False
        })
        return [default_condition]


def create_weather_dto(data: dict, weather_conditions: list[DimWeatherCondition]) \
        -> WeatherDataDTO | None:
    """
    Create WeatherDataDTO from the weather data.

    Args:
        data: The weather JSON data
        weather_conditions: List of weather condition objects

    Returns:
        WeatherDataDTO object or None if creation fails
    """
    try:
        # You can choose the primary condition (for existing code compatibility)
        # primary_weather_condition = weather_conditions[0]

        # Get main weather data with defaults
        main_data = data.get('main', {})
        wind_data = data.get('wind', {})
        clouds_data = data.get('clouds', {})

        # Create the DTO
        weather_dto = WeatherDataDTO(
            city_id=data.get('id', 0),
            temperature=safe_decimal(main_data.get('temp', 273.15)),
            feels_like=safe_decimal(main_data.get('feels_like', 273.15)),
            temp_min=safe_decimal(main_data.get('temp_min', 273.15)),
            temp_max=safe_decimal(main_data.get('temp_max', 273.15)),
            pressure=main_data.get('pressure', 1013),
            sea_level_pressure=main_data.get('sea_level', 1013),
            ground_level_pressure=main_data.get('grnd_level', 1013),
            humidity=main_data.get('humidity', 50),
            wind_speed=safe_decimal(wind_data.get('speed', 0)),
            wind_degrees=wind_data.get('deg', 0),
            wind_gust=safe_decimal(wind_data.get('gust', 0)),
            cloudiness=clouds_data.get('all', 0),
            visibility=data.get('visibility', 10000),
            weather_condition_ids=[condition.condition_id for condition in weather_conditions],
            calculation_time=safe_datetime(data.get('dt', int(datetime.now().timestamp())))
        )
        print(f"\n{weather_dto = }, {type(weather_dto) = }")

        # Process precipitation data
        process_precipitation(data, weather_dto)

        return weather_dto
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error creating weather DTO: {e}")
        return None


def process_precipitation(data: dict, dto: WeatherDataDTO) -> None:
    """
    Process precipitation data and update the DTO.

    Args:
        data: The weather JSON data
        dto: The WeatherDataDTO to update
    """
    try:
        precipitation_type = PrecipitationType.NONE
        precipitation_volume_1h = None

        if 'rain' in data:
            precipitation_type = PrecipitationType.RAIN
            precipitation_volume_1h = safe_decimal(data['rain'].get('1h', 0))
        elif 'snow' in data:
            precipitation_type = PrecipitationType.SNOW
            precipitation_volume_1h = safe_decimal(data['snow'].get('1h', 0))

        dto.precipitation_type = precipitation_type
        dto.precipitation_volume_1h = precipitation_volume_1h

        if precipitation_type != PrecipitationType.NONE:
            print(f"Precipitation detected: {precipitation_type.value}"
                  f"- {precipitation_volume_1h}mm/h")
    except (TypeError, AttributeError, ValueError) as e:
        print(f"Warning: Error processing precipitation data: {e}")
        # Non-critical, continue processing


def create_fact_object(data: dict, city: DimCity, dto: WeatherDataDTO,
                       weather_conditions: list[DimWeatherCondition]) -> FactCurrentWeather | None:
    """
    Create the FactCurrentWeather object.

    Args:
        data: The weather JSON data
        city: The city dimension object
        dto: The weather data transfer object
        weather_conditions: List of weather condition objects

    Returns:
        FactCurrentWeather object or None if creation fails
    """
    try:
        # Use the WeatherMapper to create dimension objects
        # Create dimensions from DTO
        dimensions = WeatherMapper.dto_to_dimensions(dto)
        print("\nSuccessfully created dimension objects from DTO")
        print(f"{dimensions = }, {type(dimensions) = }")

        # Create date and time dimensions
        calculation_datetime = safe_datetime(data.get('dt', int(datetime.now().timestamp())))
        date_dim = DateDimensionFactory.create_from_date(calculation_datetime.date())
        time_dim = TimeDimensionFactory.create_from_time(calculation_datetime.time())
        # print(f"Created date dimension for {date_dim.date_id} and time dimension for"
        #      f"{time_dim.time_id}")
        # print(f"FactCurrentWeather fields: {[field.name for field in
        #                                     fields(FactCurrentWeather)]}")

        # Create the fact object
        current_weather_fact = FactCurrentWeather(
            weather_id=1,
            city_id=city.city_id,
            condition_ids=[condition.condition_id for condition in weather_conditions],
            wind_id=dimensions['wind'].wind_id,
            temperature_id=dimensions['temperature'].temperature_id,
            pressure_id=dimensions['pressure'].pressure_id,
            humidity_id=dimensions['humidity'].humidity_id,
            precipitation_id=dimensions['precipitation'].precipitation_id,
            date_id=date_dim.date_id,
            time_id=time_dim.time_id,
            calculation_time=calculation_datetime,
            cloudiness=data.get('clouds', {}).get('all', 0),
            visibility=data.get('visibility', 10000),
            latitude=city.latitude,
            longitude=city.longitude,
            created_at=datetime.now(),
            response_code=data.get('cod', 200),
            base_param=data.get('base', ''),
            source_system="OpenWeatherMap"
        )

        print("\nSuccessfully created current weather fact object")
        print(f"{current_weather_fact = }, {type(current_weather_fact) = }")

        return current_weather_fact
    except (ValueError, TypeError, KeyError, AttributeError) as e:
        print(f"Error creating fact object: {e}")
        return None


def process_current_weather() -> FactCurrentWeather | None:
    """
    Process current weather JSON files to create a FactCurrentWeather object.

    Returns:
        FactCurrentWeather | None: The processed current weather fact object or
        None if processing fails
    """
    # Step 1: Find and load the weather file
    data, _ = find_and_load_weather_file()
    if not data:
        return None

    # Step 2: Create the city object
    city = create_city_object(data)
    if not city:
        return None

    # Step 3: Create weather condition objects
    weather_conditions = create_weather_conditions(data)

    # Step 4: Create the weather data transfer object
    weather_dto = create_weather_dto(data, weather_conditions)
    if not weather_dto:
        return None

    # Step 5: Create the fact object
    return create_fact_object(data, city, weather_dto, weather_conditions)


def main() -> None:
    """

    :return:
    """
    try:
        print("Processing weather data...")
        # Process current weather
        process_current_weather()
        # file_path = f"{output_files}forcast_{location}_{formatted_datetime}.json"
    except (ValueError, TypeError) as e:
        print(f"Error creating dimensions or fact object: {e}")
    except KeyError as e:
        print(f"Missing required key: {e}")
    except AttributeError as e:
        print(f"Missing attribute: {e}")


if __name__ == '__main__':
    main()
