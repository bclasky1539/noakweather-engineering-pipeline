"""
This python script gets the current weather from the openweather API.
"""
import json
import os
from datetime import datetime
from pathlib import Path
from typing import Any, Self, Dict, Optional, List

from dotenv import load_dotenv

from openweathermap.datasets.forecast import FactForecast
from openweathermap.datasets.shared import (WeatherDataDTO, WeatherMapper, DateDimensionFactory,
                                            TimeDimensionFactory, DimDate, DimTime)
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


# pylint: disable=too-many-instance-attributes
class ForecastItemContext:
    """
    A context class to hold related forecast item data for processing.
    This helps reduce the number of parameters needed in functions.
    """
    weather_conditions: list[Any]

    def __init__(self, forecast_item: dict, idx: int, city_id: int):
        """
        Initialize the context with the minimum required data.
        Additional data will be added as processing progresses.

        Args:
            forecast_item: The forecast item data
            idx: The index of the forecast item
            city_id: The ID of the city
        """
        self.forecast_item: Dict[str, Any] = forecast_item
        self.idx: int = idx
        self.city_id: int = city_id
        self.forecast_datetime = safe_datetime(
            forecast_item.get('dt', int(datetime.now().timestamp()))
        )

        # These will be populated during processing
        self.weather_conditions: List[DimWeatherCondition] = []
        self.dimensions: Optional[Dict[str, Any]] = None
        self.date_dim: Optional[DimDate] = None
        self.time_dim: Optional[DimTime] = None

    def add_weather_conditions(self, conditions: list[DimWeatherCondition]) -> Self:
        """Add weather conditions to the context.
        :param conditions:
        :return:
        """
        self.weather_conditions = conditions
        return self

    def add_dimensions(self, dims: dict) -> Self:
        """Add dimensions to the context.
        :param dims:
        :return:
        """
        self.dimensions = dims
        return self

    def add_date_time_dims(self, date_dim: DimDate, time_dim: DimTime) -> Self:
        """Add date and time dimensions to the context.
        :param date_dim:
        :param time_dim:
        :return:
        """
        self.date_dim = date_dim
        self.time_dim = time_dim
        return self


def find_and_load_forecast_file() -> tuple[dict, str] | tuple[None, None]:
    """
    Find and load the latest forecast JSON file.

    Returns:
        tuple: (data, filename) or (None, None) if no file found or loading fails
    """
    try:
        forecast_files = list(Path(f"{output_files}").glob("forecast_*.json"))
        if not forecast_files:
            print("No forecast files found")
            return None, None

        filename = forecast_files[0]
        print(f"{type(forecast_files) = }, {forecast_files = }, {filename = }")

        # Load the JSON data
        with open(filename, 'r', encoding="utf-8") as file:
            data = json.load(file)

        print(f"Successfully loaded forecast data from {filename}")
        return data, str(filename)
    except (FileNotFoundError, PermissionError, IOError) as e:
        print(f"Error accessing file or directory: {e}")
    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON file: {e}")

    return None, None  # Single return point for failure cases


def create_forecast_city_object(data: dict) -> DimCity | None:
    """
    Create city dimension object from forecast data.

    Args:
        data: The forecast JSON data

    Returns:
        DimCity object or None if creation fails
    """
    try:
        city_data = {
            'city_id': data.get('city', {}).get('id', 0),
            'name': data.get('city', {}).get('name', 'Unknown'),
            'country': data.get('city', {}).get('country', 'Unknown'),
            'timezone': data.get('city', {}).get('timezone', 0),
            'latitude': safe_decimal(data.get('city', {}).get('coord', {}).get('lat', 0)),
            'longitude': safe_decimal(data.get('city', {}).get('coord', {}).get('lon', 0)),
            'state': data.get('sys', {}).get('state', 'Unknown'),
            'population': data.get('city', {}).get('population', 0),
            'sunrise': safe_datetime(data.get('city', {}).get('sunrise', 0)),
            'sunset': safe_datetime(data.get('city', {}).get('sunset', 0))
        }

        return DimCity.from_dict(city_data)
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error processing city data: {e}")
        return None


def create_forecast_weather_conditions(forecast_item: dict) -> list[DimWeatherCondition]:
    """
    Create weather condition objects from a forecast item.

    Args:
        forecast_item: A single forecast item from the forecast data

    Returns:
        List of DimWeatherCondition objects
    """
    weather_conditions = []

    try:
        for weather_item in forecast_item.get('weather', []):
            weather_condition_data = {
                'condition_id': weather_item.get('id', 800),
                'condition_main': weather_item.get('main', 'Unknown'),
                'description': weather_item.get('description', 'No description available'),
                'icon_code': weather_item.get('icon', '01d'),
                'is_precipitation': weather_item.get('main', '') in ['Rain', 'Snow', 'Drizzle']
            }
            # print(f"{weather_condition_data = }, {type(weather_condition_data) = }")
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

        # print(f"{type(weather_conditions) = }, {weather_conditions = }")

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


def create_forecast_weather_dto(forecast_item: dict, city_id: int,
                                weather_conditions: list[DimWeatherCondition]) \
        -> WeatherDataDTO | None:
    """
    Create WeatherDataDTO from a forecast item.

    Args:
        forecast_item: A single forecast item from the forecast data
        city_id: The city ID
        weather_conditions: List of weather condition objects

    Returns:
        WeatherDataDTO object or None if creation fails
    """
    try:
        # Get main forecast data with defaults
        main_data = forecast_item.get('main', {})
        wind_data = forecast_item.get('wind', {})
        clouds_data = forecast_item.get('clouds', {})

        # Create the DTO
        weather_dto = WeatherDataDTO(
            city_id=city_id,
            temperature=safe_decimal(main_data.get('temp', 273.15)),
            feels_like=safe_decimal(main_data.get('feels_like', 273.15)),
            temp_min=safe_decimal(main_data.get('temp_min', 273.15)),
            temp_max=safe_decimal(main_data.get('temp_max', 273.15)),
            pressure=main_data.get('pressure', 1013),
            humidity=main_data.get('humidity', 50),
            wind_speed=safe_decimal(wind_data.get('speed', 0)),
            wind_degrees=wind_data.get('deg', 0),
            wind_gust=safe_decimal(wind_data.get('gust', 0)),
            cloudiness=clouds_data.get('all', 0),
            visibility=forecast_item.get('visibility', 10000),
            weather_condition_ids=[condition.condition_id for condition in weather_conditions],
            calculation_time=safe_datetime(forecast_item.get('dt', int(datetime.now().timestamp())))
        )

        # Process precipitation data for forecast (uses 3h instead of 1h)
        process_forecast_precipitation(forecast_item, weather_dto)

        return weather_dto
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error creating weather DTO: {e}")
        return None


def process_forecast_precipitation(forecast_item: dict, dto: WeatherDataDTO) -> None:
    """
    Process precipitation data for a forecast item and update the DTO.

    Args:
        forecast_item: A single forecast item from the forecast data
        dto: The WeatherDataDTO to update
    """
    try:
        precipitation_type = PrecipitationType.NONE
        precipitation_volume_3h = None

        if 'rain' in forecast_item:
            precipitation_type = PrecipitationType.RAIN
            precipitation_volume_3h = safe_decimal(forecast_item['rain'].get('3h', 0))
        elif 'snow' in forecast_item:
            precipitation_type = PrecipitationType.SNOW
            precipitation_volume_3h = safe_decimal(forecast_item['snow'].get('3h', 0))

        dto.precipitation_type = precipitation_type
        dto.precipitation_volume_3h = precipitation_volume_3h
    except (TypeError, AttributeError, ValueError) as e:
        print(f"Warning: Error processing precipitation data: {e}")
        # Non-critical, continue processing


def create_forecast_fact(context: ForecastItemContext) -> FactForecast | None:
    """
    Create a FactForecast object for a forecast item.

    Args:
        context: A context object containing all necessary data for the forecast item

    Returns:
        FactForecast object or None if creation fails
    """
    try:
        # Check if required attributes are set
        if context.dimensions is None:
            print("Error: dimensions not set in context")
            return None

        if context.date_dim is None or context.time_dim is None:
            print("Error: date_dim or time_dim not set in context")
            return None

        clouds_data = context.forecast_item.get('clouds', {})

        # Extract probability of precipitation if available
        pop = context.forecast_item.get('pop', 0) * 100  # Convert from 0-1 scale to percentage

        forecast_fact = FactForecast(
            forecast_item_id=context.idx + 1,  # This would be assigned by a database
            forecast_id=1,  # This would be a group ID for all items in this forecast
            city_id=context.city_id,
            condition_ids=[condition.condition_id for condition in context.weather_conditions],
            wind_id=context.dimensions['wind'].wind_id,
            temperature_id=context.dimensions['temperature'].temperature_id,
            pressure_id=context.dimensions['pressure'].pressure_id,
            humidity_id=context.dimensions['humidity'].humidity_id,
            precipitation_id=context.dimensions['precipitation'].precipitation_id,
            date_id=context.date_dim.date_id,
            time_id=context.time_dim.time_id,
            forecast_time=context.forecast_datetime,
            cloudiness=clouds_data.get('all', 0),
            visibility=context.forecast_item.get('visibility', 10000),
            probability_of_precipitation=pop,
            created_at=datetime.now()
        )
        # print("Successfully created forecast fact object")
        # print(f"{forecast_fact = }, {type(forecast_fact) = }")

        return forecast_fact
    except (ValueError, TypeError, KeyError, AttributeError) as e:
        print(f"Error creating forecast fact: {e}")
        return None


def process_forecast_stages(context: ForecastItemContext) -> bool:
    """
    Execute processing stages for a forecast item context.

    This function handles the step-by-step processing of a forecast item,
    updating the context object with results from each stage.

    Args:
        context: The forecast item context to process

    Returns:
        bool: True if processing succeeded, False otherwise
    """
    # Step 1: Process weather conditions
    weather_conditions = create_forecast_weather_conditions(context.forecast_item)
    if not weather_conditions:
        return False
    context.add_weather_conditions(weather_conditions)

    # Step 2: Create weather DTO
    weather_dto = create_forecast_weather_dto(
        context.forecast_item, context.city_id, context.weather_conditions
    )
    if not weather_dto:
        return False

    # Step 3: Create dimensions
    dimensions = WeatherMapper.dto_to_dimensions(weather_dto)
    if not dimensions:
        return False
    context.add_dimensions(dimensions)

    # Step 4: Create date and time dimensions
    date_dim = DateDimensionFactory.create_from_date(context.forecast_datetime.date())
    time_dim = TimeDimensionFactory.create_from_time(context.forecast_datetime.time())
    if not date_dim or not time_dim:
        return False
    context.add_date_time_dims(date_dim, time_dim)

    # All stages succeeded
    return True


def process_forecast_item(forecast_item: dict, idx: int, city: DimCity) -> dict | None:
    """
    Process a single forecast item.

    Args:
        forecast_item: A single forecast item from the forecast data
        idx: The index of the forecast item
        city: The city dimension object

    Returns:
        Dictionary with processed forecast data or None if processing fails
    """
    try:
        # Create a context object with minimum required data
        context = ForecastItemContext(forecast_item, idx, city.city_id)

        # Process all stages
        if not process_forecast_stages(context):
            return None

        # Create forecast fact
        forecast_fact = create_forecast_fact(context)
        if not forecast_fact:
            return None

        # Return the processed forecast item
        return {
            'weather_condition': context.weather_conditions,
            'dimensions': context.dimensions,
            'date_dim': context.date_dim,
            'time_dim': context.time_dim,
            'forecast_fact': forecast_fact
        }

    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error processing forecast item {idx} - data error: {e}")
        return None
    except KeyError as e:
        print(f"Error processing forecast item {idx} - missing key: {e}")
        return None


# pylint: disable=too-many-branches
def process_forecast_item_old(forecast_item: dict, idx: int, city: DimCity) -> dict | None:
    """
    Process a single forecast item.

    Args:
        forecast_item: A single forecast item from the forecast data
        idx: The index of the forecast item
        city: The city dimension object

    Returns:
        Dictionary with processed forecast data or None if processing fails
    """
    # Create a context object with minimum required data
    context = ForecastItemContext(forecast_item, idx, city.city_id)

    # Initialize variables
    weather_dto = None

    # Use success flag to control flow and reduce return statements
    success = True

    # Step 1: Process weather conditions
    try:
        weather_conditions = create_forecast_weather_conditions(forecast_item)
        context.add_weather_conditions(weather_conditions)
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error processing weather conditions for item {idx}: {e}")
        success = False

    # Step 2: Create weather DTO if previous step succeeded
    if success:
        try:
            weather_dto = create_forecast_weather_dto(
                forecast_item, city.city_id, context.weather_conditions
            )
            if not weather_dto:
                success = False
        except (ValueError, TypeError, AttributeError) as e:
            print(f"Error creating weather DTO for item {idx}: {e}")
            success = False

    # Step 3: Create dimensions if previous step succeeded
    if success:
        try:
            if weather_dto is None:
                print(f"Error: weather_dto is None for item {idx}")
                success = False
            else:
                dimensions = WeatherMapper.dto_to_dimensions(weather_dto)
                context.add_dimensions(dimensions)
        except (ValueError, TypeError, KeyError, AttributeError) as e:
            print(f"Error creating dimensions for item {idx}: {e}")
            success = False

    # Step 4: Create date and time dimensions if previous step succeeded
    if success:
        try:
            date_dim = DateDimensionFactory.create_from_date(context.forecast_datetime.date())
            time_dim = TimeDimensionFactory.create_from_time(context.forecast_datetime.time())
            context.add_date_time_dims(date_dim, time_dim)
        except (ValueError, TypeError, AttributeError) as e:
            print(f"Error creating date/time dimensions for item {idx}: {e}")
            success = False

    # Step 5: Create forecast fact if all previous steps succeeded
    forecast_fact = None
    if success:
        try:
            forecast_fact = create_forecast_fact(context)
            if not forecast_fact:
                success = False
        except (ValueError, TypeError, KeyError, AttributeError) as e:
            print(f"Error creating forecast fact for item {idx}: {e}")
            success = False

    # Return the processed forecast item or None if any step failed
    if success and forecast_fact:
        return {
            'weather_condition': context.weather_conditions,
            'dimensions': context.dimensions,
            'date_dim': context.date_dim,
            'time_dim': context.time_dim,
            'forecast_fact': forecast_fact
        }

    return None


def process_forecast() -> list[dict[str, object]] | None:
    """
    Process forecast JSON files to create forecast objects.

    Returns:
        list[dict[str, object]] | None: List of processed forecast items or None if processing fails
    """
    # Use success flag to control flow and reduce return statements
    result = None

    # Step 1: Find and load the forecast file
    data, _ = find_and_load_forecast_file()
    if not data:
        return None

    # Step 2: Create the city object
    try:
        city = create_forecast_city_object(data)
        if not city:
            return None
    except (ValueError, TypeError, AttributeError) as e:
        print(f"Error creating city object: {e}")
        return None

    # Step 3: Get forecast list
    forecast_list = data.get('list', [])
    if not forecast_list:
        print("Forecast data contains no forecast items")
        return None

    print(f"\nProcessing {len(forecast_list)} forecast items")

    # Step 4: Process each forecast item
    forecast_results = []
    for idx, forecast_item in enumerate(forecast_list):
        result = process_forecast_item(forecast_item, idx, city)
        if result:
            forecast_results.append(result)

    # Step 5: Check if any items were processed successfully
    if not forecast_results:
        print("No forecast items were successfully processed")
        return None

    print("\n\nSuccessfully created forecast_results object")
    print(f"Processed {len(forecast_results)} forecast items")
    print(f"{forecast_results = }, {type(forecast_results) = }")

    return forecast_results


def main() -> None:
    """

    :return:
    """
    try:
        print("Processing weather data...")
        # Process forecast
        process_forecast()
        # file_path = f"{output_files}forcast_{location}_{formatted_datetime}.json"
    except (ValueError, TypeError) as e:
        print(f"Error creating dimensions or fact object: {e}")
    except KeyError as e:
        print(f"Missing required key: {e}")
    except AttributeError as e:
        print(f"Missing attribute: {e}")


if __name__ == '__main__':
    main()
