"""
Unified Weather Processing Module

This module provides a common framework for processing both current weather and forecast data
from the OpenWeather API.
"""
import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple, Union, TypeVar, Self

from dotenv import load_dotenv

from openweathermap.datasets.current_weather import FactCurrentWeather
from openweathermap.datasets.forecast import FactForecast
from openweathermap.datasets.shared import (
    WeatherDataDTO, WeatherMapper, DateDimensionFactory, TimeDimensionFactory, DimDate, DimTime
)
from openweathermap.datasets.weather import DimCity, DimWeatherCondition, PrecipitationType
from utils.project_patterns import find_project_root, safe_decimal, safe_datetime

# Type aliases for better readability
FT = TypeVar('FT', FactCurrentWeather, FactForecast)  # FT = Fact Type
WeatherDataDict = Dict[str, Any]
ProcessedData = Dict[str, Any]

# Setup environment
project_root = find_project_root()
if project_root is None:
    raise ValueError("Could not determine project root")

load_dotenv()
output_files: str = os.path.join(project_root + os.getenv("OUTPUT_FILES", ""))

# Get current datetime for the filename or content
current_datetime = datetime.now()
formatted_datetime = current_datetime.strftime("%Y%m%d%H%M%S")


class ProcessingContext:
    """
    A context class to hold related weather data for processing.
    This helps reduce the number of parameters needed in functions.
    """

    # pylint: disable=too-many-instance-attributes
    def __init__(self, data_item: Dict[str, Any], city_id: int, item_id: int = 1):
        """
        Initialize the context with the minimum required data.

        Args:
            data_item: The weather data item (current or forecast)
            city_id: The ID of the city
            item_id: The ID of the item (1 for current weather, sequence for forecast)
        """
        self.data_item: Dict[str, Any] = data_item
        self.city_id: int = city_id
        self.item_id: int = item_id
        self.calculation_time = safe_datetime(
            data_item.get('dt', int(datetime.now().timestamp()))
        )

        # These will be populated during processing
        self.weather_conditions: List[DimWeatherCondition] = []
        self.dimensions: Optional[Dict[str, Any]] = None
        self.date_dim: Optional[DimDate] = None
        self.time_dim: Optional[DimTime] = None
        self.weather_dto: Optional[WeatherDataDTO] = None

    def add_weather_conditions(self, conditions: List[DimWeatherCondition]) -> Self:
        """Add weather conditions to the context."""
        self.weather_conditions = conditions
        return self

    def add_weather_dto(self, dto: WeatherDataDTO) -> Self:
        """Add weather DTO to the context."""
        self.weather_dto = dto
        return self

    def add_dimensions(self, dims: Dict[str, Any]) -> Self:
        """Add dimensions to the context."""
        self.dimensions = dims
        return self

    def add_date_time_dims(self, date_dim: DimDate, time_dim: DimTime) -> Self:
        """Add date and time dimensions to the context."""
        self.date_dim = date_dim
        self.time_dim = time_dim
        return self


class WeatherProcessor:
    """Base class for processing weather data."""

    def __init__(self, file_glob_pattern: str):
        """
        Initialize the processor with the file pattern to search for.

        Args:
            file_glob_pattern: Glob pattern to find weather data files
        """
        self.file_glob_pattern = file_glob_pattern

    def find_and_load_file(self) -> Tuple[Optional[Dict[str, Any]], Optional[str]]:
        """
        Find and load the latest weather data file matching the glob pattern.

        Returns:
            tuple: (data, filename) or (None, None) if no file found or loading fails
        """
        try:
            files = list(Path(output_files).glob(self.file_glob_pattern))
            if not files:
                print(f"No files found matching {self.file_glob_pattern}")
                return None, None

            filename = files[0]
            print(f"Loading data from {filename}")

            with open(filename, 'r', encoding="utf-8") as file:
                data = json.load(file)

            print(f"Successfully loaded data from {filename}")
            return data, str(filename)
        except (FileNotFoundError, PermissionError, IOError) as e:
            print(f"Error accessing file or directory: {e}")
        except json.JSONDecodeError as e:
            print(f"Failed to parse JSON file: {e}")

        return None, None

    def create_city_object(self, data: Dict[str, Any]) -> Optional[DimCity]:
        """
        Create city dimension object from weather data.

        Args:
            data: The weather JSON data

        Returns:
            DimCity object or None if creation fails
        """
        try:
            # Both processors access city data differently, so we need to handle both formats
            if 'city' in data:
                # Forecast data format
                city_data = {
                    'city_id': data.get('city', {}).get('id', 0),
                    'name': data.get('city', {}).get('name', 'Unknown'),
                    'country': data.get('city', {}).get('country', 'Unknown'),
                    'timezone': data.get('city', {}).get('timezone', 0),
                    'latitude': safe_decimal(data.get('city', {}).get('coord', {}).get('lat', 0)),
                    'longitude': safe_decimal(data.get('city', {}).get('coord', {}).get('lon', 0)),
                    'state': data.get('city', {}).get('state', 'Unknown'),
                    'population': data.get('city', {}).get('population', 0),
                    'sunrise': safe_datetime(data.get('city', {}).get('sunrise', 0)),
                    'sunset': safe_datetime(data.get('city', {}).get('sunset', 0))
                }
            else:
                # Current weather data format
                city_data = {
                    'city_id': data.get('id', 0),
                    'name': data.get('name', 'Unknown'),
                    'country': data.get('sys', {}).get('country', 'Unknown'),
                    'timezone': data.get('timezone', 0),
                    'latitude': safe_decimal(data.get('coord', {}).get('lat', 0)),
                    'longitude': safe_decimal(data.get('coord', {}).get('lon', 0)),
                    'state': data.get('sys', {}).get('state', 'Unknown'),
                    'population': 0,
                    'sunrise': safe_datetime(data.get('sys', {}).get('sunrise', 0)),
                    'sunset': safe_datetime(data.get('sys', {}).get('sunset', 0))
                }

            city = DimCity.from_dict(city_data)
            print(f"Created city dimension for {city.name}, {city.country}")
            print(f"{city = }")
            return city
        except (ValueError, TypeError, AttributeError) as e:
            print(f"Error processing city data: {e}")
            return None

    def create_weather_conditions(self, data_item: Dict[str, Any]) -> List[DimWeatherCondition]:
        """
        Create weather condition objects from weather data.

        Args:
            data_item: The weather data item (full current weather or a forecast item)

        Returns:
            List of DimWeatherCondition objects
        """
        weather_conditions = []

        try:
            for weather_item in data_item.get('weather', []):
                weather_condition_data = {
                    'condition_id': weather_item.get('id', 800),
                    'condition_main': weather_item.get('main', 'Unknown'),
                    'description': weather_item.get('description', 'No description available'),
                    'icon_code': weather_item.get('icon', '01d'),
                    'is_precipitation': weather_item.get('main', '') in ['Rain', 'Snow', 'Drizzle'],
                    'is_extreme': False
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

    def create_weather_dto(self, context: ProcessingContext) -> Optional[WeatherDataDTO]:
        """
        Create WeatherDataDTO from weather data.

        Args:
            context: The processing context containing data and city information

        Returns:
            WeatherDataDTO object or None if creation fails
        """
        try:
            data_item = context.data_item

            # Get main weather data with defaults
            main_data = data_item.get('main', {})
            wind_data = data_item.get('wind', {})
            clouds_data = data_item.get('clouds', {})

            # Create the DTO
            weather_dto = WeatherDataDTO(
                city_id=context.city_id,
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
                visibility=data_item.get('visibility', 10000),
                weather_condition_ids=[condition.condition_id
                                       for condition in context.weather_conditions],
                calculation_time=context.calculation_time
            )

            print(f"\n{weather_dto = }, {type(weather_dto) = }")

            return weather_dto
        except (ValueError, TypeError, AttributeError) as e:
            print(f"Error creating weather DTO: {e}")
            return None

    def process_precipitation(self, context: ProcessingContext) -> None:
        """
        Process precipitation data and update the DTO.
        Must be implemented by subclasses.

        Args:
            context: The processing context
        """
        raise NotImplementedError("Subclasses must implement process_precipitation")

    def create_dimensions(self, context: ProcessingContext) -> bool:
        """
        Create all dimensions for the weather data.

        Args:
            context: The processing context

        Returns:
            bool: Success or failure
        """
        try:
            if context.weather_dto is None:
                print("Error: weather_dto is None")
                return False

            # Create dimensions from DTO
            dimensions = WeatherMapper.dto_to_dimensions(context.weather_dto)
            context.add_dimensions(dimensions)

            # Create date and time dimensions
            date_dim = DateDimensionFactory.create_from_date(context.calculation_time.date())
            time_dim = TimeDimensionFactory.create_from_time(context.calculation_time.time())
            context.add_date_time_dims(date_dim, time_dim)

            return True
        except (ValueError, TypeError, KeyError, AttributeError) as e:
            print(f"Error creating dimensions: {e}")
            return False

    def create_fact_object(self, context: ProcessingContext) -> (
            Optional)[Union[FactCurrentWeather, FactForecast]]:
        """
        Create the fact table entry.
        Must be implemented by subclasses.

        Args:
            context: The processing context

        Returns:
            Fact object or None if creation fails
        """
        raise NotImplementedError("Subclasses must implement create_fact_object")

    # pylint: disable=too-many-return-statements
    def process_item(self, data_item: Dict[str, Any], city: DimCity, item_id: int = 1) -> (
            Optional)[ProcessedData]:
        """
        Process a single weather data item.

        Args:
            data_item: The weather data item
            city: The city dimension object
            item_id: The ID of the item

        Returns:
            ProcessedData or None if processing fails
        """
        try:
            # Create context
            context = ProcessingContext(data_item, city.city_id, item_id)

            # Step 1: Process weather conditions
            weather_conditions = self.create_weather_conditions(data_item)
            if not weather_conditions:
                print("Failed to create weather conditions")
                return None
            context.add_weather_conditions(weather_conditions)

            # Step 2: Create weather DTO
            weather_dto = self.create_weather_dto(context)
            if not weather_dto:
                print("Failed to create weather DTO")
                return None
            context.add_weather_dto(weather_dto)

            # Step 3: Process precipitation (specific to each type)
            self.process_precipitation(context)

            # Step 4: Create dimensions
            if not self.create_dimensions(context):
                print("Failed to create dimensions")
                return None

            # Step 5: Create fact object
            fact_object = self.create_fact_object(context)
            if not fact_object:
                print("Failed to create fact object")
                return None

            # Return the processed data
            return {
                'weather_condition': context.weather_conditions[0] if len(
                    context.weather_conditions) == 1 else context.weather_conditions,
                'dimensions': context.dimensions,
                'date_dim': context.date_dim,
                'time_dim': context.time_dim,
                'fact': fact_object
            }
        except (TypeError, ValueError) as e:
            print(f"Error processing item {item_id}: Data type or value error: {e}")
            return None
        except KeyError as e:
            print(f"Error processing item {item_id}: Missing key: {e}")
            return None
        except AttributeError as e:
            print(f"Error processing item {item_id}: Object attribute error: {e}")
            return None

    def process(self) -> Optional[Union[ProcessedData, List[ProcessedData]]]:
        """
        Process weather data.
        Must be implemented by subclasses to handle their specific processing flow.

        Returns:
            The processed data or None if processing fails
        """
        raise NotImplementedError("Subclasses must implement process")


class CurrentWeatherProcessor(WeatherProcessor):
    """Processor for current weather data."""

    def __init__(self) -> None:
        """Initialize with the current weather file pattern."""
        super().__init__("current_weather_*.json")

    def process_precipitation(self, context: ProcessingContext) -> None:
        """
        Process precipitation data for current weather and update the DTO.

        Args:
            context: The processing context
        """
        try:
            if context.weather_dto is None:
                return

            data_item = context.data_item
            precipitation_type = PrecipitationType.NONE
            precipitation_volume_1h = None

            if 'rain' in data_item:
                precipitation_type = PrecipitationType.RAIN
                precipitation_volume_1h = safe_decimal(data_item['rain'].get('1h', 0))
            elif 'snow' in data_item:
                precipitation_type = PrecipitationType.SNOW
                precipitation_volume_1h = safe_decimal(data_item['snow'].get('1h', 0))

            context.weather_dto.precipitation_type = precipitation_type
            context.weather_dto.precipitation_volume_1h = precipitation_volume_1h

            if precipitation_type != PrecipitationType.NONE:
                print(f"Precipitation detected: {precipitation_type.value}"
                      f"- {precipitation_volume_1h}mm/h")
        except (TypeError, AttributeError, ValueError) as e:
            print(f"Warning: Error processing precipitation data: {e}")
            # Non-critical, continue processing

    # pylint: disable=too-many-return-statements
    def create_fact_object(self, context: ProcessingContext) -> Optional[FactCurrentWeather]:
        """
        Create the FactCurrentWeather object.

        Args:
            context: The processing context

        Returns:
            FactCurrentWeather object or None if creation fails
        """
        try:
            # Check that required attributes are not None
            if context.dimensions is None:
                print("Error: dimensions not set in context")
                return None

            if context.date_dim is None or context.time_dim is None:
                print("Error: date_dim or time_dim not set in context")
                return None

            # Create the fact object
            current_weather_fact = FactCurrentWeather(
                weather_id=context.item_id,
                city_id=context.city_id,
                condition_ids=[condition.condition_id for condition in context.weather_conditions],
                wind_id=context.dimensions['wind'].wind_id,
                temperature_id=context.dimensions['temperature'].temperature_id,
                pressure_id=context.dimensions['pressure'].pressure_id,
                humidity_id=context.dimensions['humidity'].humidity_id,
                precipitation_id=context.dimensions['precipitation'].precipitation_id,
                date_id=context.date_dim.date_id,
                time_id=context.time_dim.time_id,
                calculation_time=context.calculation_time,
                cloudiness=context.data_item.get('clouds', {}).get('all', 0),
                visibility=context.data_item.get('visibility', 10000),
                latitude=safe_decimal(context.data_item.get('coord', {}).get('lat', 0)),
                longitude=safe_decimal(context.data_item.get('coord', {}).get('lon', 0)),
                created_at=datetime.now(),
                response_code=context.data_item.get('cod', 200),
                base_param=context.data_item.get('base', ''),
                source_system="OpenWeatherMap"
            )

            print("Successfully created current weather fact object")
            print(f"{current_weather_fact = }, {type(current_weather_fact) = }")

            return current_weather_fact
        except ValueError as e:
            print(f"Error creating fact object: Value error: {e}")
            return None
        except TypeError as e:
            print(f"Error creating fact object: Type error: {e}")
            return None
        except KeyError as e:
            print(f"Error creating fact object: Missing key: {e}")
            return None
        except AttributeError as e:
            print(f"Error creating fact object: Attribute error: {e}")
            return None

    def process(self) -> Optional[ProcessedData]:
        """
        Process current weather data.

        Returns:
            ProcessedData or None if processing fails
        """
        # Step 1: Find and load the weather file
        data, _ = self.find_and_load_file()
        if not data:
            return None

        # Step 2: Create the city object
        city = self.create_city_object(data)
        if not city:
            return None

        # Step 3: Process the current weather data
        return self.process_item(data, city)


class ForecastProcessor(WeatherProcessor):
    """Processor for forecast weather data."""

    def __init__(self) -> None:
        """Initialize with the forecast file pattern."""
        super().__init__("forecast_*.json")

    def process_precipitation(self, context: ProcessingContext) -> None:
        """
        Process precipitation data for forecast and update the DTO.

        Args:
            context: The processing context
        """
        try:
            if context.weather_dto is None:
                return

            data_item = context.data_item
            precipitation_type = PrecipitationType.NONE
            precipitation_volume_3h = None

            if 'rain' in data_item:
                precipitation_type = PrecipitationType.RAIN
                precipitation_volume_3h = safe_decimal(data_item['rain'].get('3h', 0))
            elif 'snow' in data_item:
                precipitation_type = PrecipitationType.SNOW
                precipitation_volume_3h = safe_decimal(data_item['snow'].get('3h', 0))

            context.weather_dto.precipitation_type = precipitation_type
            context.weather_dto.precipitation_volume_3h = precipitation_volume_3h
        except (TypeError, AttributeError, ValueError) as e:
            print(f"Warning: Error processing precipitation data: {e}")
            # Non-critical, continue processing

    # pylint: disable=too-many-return-statements
    def create_fact_object(self, context: ProcessingContext) -> Optional[FactForecast]:
        """
        Create the FactForecast object.

        Args:
            context: The processing context

        Returns:
            FactForecast object or None if creation fails
        """
        try:
            # Check that required attributes are not None
            if context.dimensions is None:
                print("Error: dimensions not set in context")
                return None

            if context.date_dim is None or context.time_dim is None:
                print("Error: date_dim or time_dim not set in context")
                return None

            # Extract probability of precipitation if available
            pop = context.data_item.get('pop', 0) * 100  # Convert from 0-1 scale to percentage

            # Create the forecast fact
            forecast_fact = FactForecast(
                forecast_item_id=context.item_id,
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
                forecast_time=context.calculation_time,
                cloudiness=context.data_item.get('clouds', {}).get('all', 0),
                visibility=context.data_item.get('visibility', 10000),
                probability_of_precipitation=pop,
                created_at=datetime.now(),
                source_system="OpenWeatherMap"
            )

            return forecast_fact
        except ValueError as e:
            print(f"Error creating forecast fact: Value error: {e}")
            return None
        except TypeError as e:
            print(f"Error creating forecast fact: Type error: {e}")
            return None
        except KeyError as e:
            print(f"Error creating forecast fact: Missing key: {e}")
            return None
        except AttributeError as e:
            print(f"Error creating forecast fact: Attribute error: {e}")
            return None

    def process(self) -> Optional[List[ProcessedData]]:
        """
        Process forecast data.

        Returns:
            List of ProcessedData or None if processing fails
        """
        # Step 1: Find and load the forecast file
        data, _ = self.find_and_load_file()
        if not data:
            return None

        # Step 2: Create the city object
        city = self.create_city_object(data)
        if not city:
            return None

        # Step 3: Get forecast list
        forecast_list = data.get('list', [])
        if not forecast_list:
            print("Forecast data contains no forecast items")
            return None

        print(f"Processing {len(forecast_list)} forecast items")

        # Step 4: Process each forecast item
        forecast_results = []
        for idx, forecast_item in enumerate(forecast_list):
            result = self.process_item(forecast_item, city, idx + 1)
            if result:
                forecast_results.append(result)

        # Step 5: Check if any items were processed successfully
        if not forecast_results:
            print("No forecast items were successfully processed")
            return None

        print(f"Successfully processed {len(forecast_results)} forecast items")
        return forecast_results
