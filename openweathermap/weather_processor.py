"""
This python script gets the current weather from the openweather API.
"""
import json
import os
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv

from openweathermap.datasets.current_weather import FactCurrentWeather
from openweathermap.datasets.forecast import FactForecast
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
def process_current_weather() -> FactCurrentWeather | None:
    """

    :return:
    """
    try:
        cw_json_files = list(Path(f"{output_files}").glob("current_weather_*.json"))
        if len(cw_json_files) == 0:
            print("No current weather files found")
            return None
        print(f"{cw_json_files = }, {cw_json_files[0] = }")
        print(f"{type(cw_json_files) = }")

        # Load the JSON data
        with open(cw_json_files[0], 'r', encoding="utf-8") as file:
            try:
                data = json.load(file)
                print(f"{data = }")
                print(f"{type(data)= }")
            except json.JSONDecodeError as e:
                print(f"Failed to parse JSON file {cw_json_files[0]}: {e}")
                return None

        print(f"Successfully loaded current weather data from {cw_json_files[0]}")

        try:
            # Extract the city information
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
            # print(f"{city_data = }, {type(city_data) = }")

            # Create the city dimension object
            city = DimCity.from_dict(city_data)
            # print(f"{city = }, {type(city) = }")

            # Extract the main weather conditions
            weather_conditions = []
            for weather_item in data.get('weather', []):
                # print(f"{type(weather_item) = }, {weather_item = }")
                weather_condition_data = {
                    'condition_id': weather_item.get('id', 800),  # Default to clear sky
                    'condition_main': weather_item.get('main', 'Unknown'),
                    'description': weather_item.get('description', 'No description available'),
                    'icon_code': weather_item.get('icon', '01d'),
                    'is_precipitation': weather_item.get('main', '') in ['Rain', 'Snow', 'Drizzle']
                }
                # print(f"{weather_condition_data = }, {type(weather_condition_data) = }")
                # weather_conditions.append(DimWeatherCondition.from_dict(weather_condition_data))
                # Create individual DimWeatherCondition objects
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
                # Add to the weather condition dimension object
                weather_conditions.append(DimWeatherCondition.from_dict(default_weather_data))

            print(f"{type(weather_conditions) = }, {weather_conditions = }")

            # You can choose the primary condition (for existing code compatibility)
            # primary_weather_condition = weather_conditions[0]

            # Get main weather data with defaults
            main_data = data.get('main', {})
            wind_data = data.get('wind', {})
            clouds_data = data.get('clouds', {})

            # Create a WeatherDataDTO from the JSON data
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
                # weather_condition_id=weather_data.get('id', 800),
                # weather_conditions=weather_conditions or [],
                # Do not pass DimWeatherCondition objects, but keep their IDs
                weather_condition_ids=[condition.condition_id for condition in weather_conditions],
                calculation_time=safe_datetime(data.get('dt', int(datetime.now().timestamp())))
            )
            print(f"\n{weather_dto = }, {type(weather_dto) = }")

            # Determine precipitation type and volume if present
            precipitation_type = PrecipitationType.NONE
            precipitation_volume_1h = None

            try:
                if 'rain' in data:
                    precipitation_type = PrecipitationType.RAIN
                    precipitation_volume_1h = safe_decimal(data['rain'].get('1h', 0))
                elif 'snow' in data:
                    precipitation_type = PrecipitationType.SNOW
                    precipitation_volume_1h = safe_decimal(data['snow'].get('1h', 0))

                weather_dto.precipitation_type = precipitation_type
                weather_dto.precipitation_volume_1h = precipitation_volume_1h

                if precipitation_type != PrecipitationType.NONE:
                    print(f"Precipitation detected:"
                          f"{precipitation_type.value} - {precipitation_volume_1h}mm/h")
            except Exception as e:
                print(f"Error processing precipitation data: {e}")

            # Use the WeatherMapper to create dimension objects
            # try:
            dimensions = WeatherMapper.dto_to_dimensions(weather_dto)
            print("\nSuccessfully created dimension objects from DTO")
            print(f"{dimensions = }, {type(dimensions) = }")
            # except Exception as e:
            #    print(f"Failed to map DTO to dimensions: {e}")
            #    raise

            # Create date and time dimensions
            calculation_datetime = safe_datetime(data.get('dt', int(datetime.now().timestamp())))

            try:
                date_dim = DateDimensionFactory.create_from_date(calculation_datetime.date())
                time_dim = TimeDimensionFactory.create_from_time(calculation_datetime.time())
                # print(f"Created date dimension for {date_dim.date_id} and time dimension for {time_dim.time_id}")
            except Exception as e:
                print(f"Failed to create date/time dimensions: {e}")
                raise

            # print(f"FactCurrentWeather fields: {[field.name for field in fields(FactCurrentWeather)]}")

            # Create the fact table entry
            try:
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
                    cloudiness=clouds_data.get('all', 0),
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
            except Exception as e:
                print(f"Failed to create current weather fact: {e}")
                raise

        except KeyError as e:
            print(f"Missing required key in data: {e}")
            return None
        except ImportError as e:
            print(f"Error processing current weather data: {e}")
            return None

    except (FileNotFoundError, FileExistsError, IOError) as e:
        print(f"An error occurred: {e}")
        return None

    return current_weather_fact


# (list[dict[str, list[DimWeatherCondition] | DimTime | FactForecast |
#                                          dict[str, Any] | DimDate]] | None)
def process_forecast() -> list[dict[str, object]] | None:
    """

    :return:
    """
    try:
        cw_json_files = list(Path(f"{output_files}").glob("forecast_*.json"))

        if len(cw_json_files) == 0:
            print("No forecast files found")
            return None

        print(f"{cw_json_files = }, {cw_json_files[0] = }")
        print(f"{type(cw_json_files) = }")

        # Load the JSON data
        with open(cw_json_files[0], 'r', encoding="utf-8") as file:
            try:
                data = json.load(file)
                print(f"{type(data)= }, {data = }")
            except json.JSONDecodeError as e:
                print(f"Failed to parse JSON file {cw_json_files[0]}: {e}")
                return None

        print(f"Successfully loaded current weather data from {cw_json_files[0]}")

        try:
            # Extract the city information
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

            # Create the city dimension object
            city = DimCity.from_dict(city_data)
            # print(f"Created city dimension for {city.name}, {city.country}")

            # Process each forecast entry
            forecast_results = []
            forecast_list = data.get('list', [])
            # print(f"{len(forecast_list) = }, {forecast_list = }")

            if not forecast_list:
                print("Forecast data contains no forecast items")
                return None

            print(f"\nProcessing {len(forecast_list)} forecast items")

            for idx, forecast_item in enumerate(forecast_list):
                try:
                    # Extract the main weather condition
                    # weather_data = forecast_item.get('weather', [{}])[0] if forecast_item.get('weather') else {}
                    # weather_condition_data = {
                    #    'condition_id': weather_data.get('id', 800),
                    #    'condition_main': weather_data.get('main', 'Unknown'),
                    #    'description': weather_data.get('description', 'No description available'),
                    #    'icon_code': weather_data.get('icon', '01d'),
                    #    'is_precipitation': weather_data.get('main', '') in ['Rain', 'Snow', 'Drizzle']
                    # }
                    #
                    # weather_condition = DimWeatherCondition.from_dict(weather_condition_data)

                    # Extract the main weather conditions
                    weather_conditions = []
                    # type(weather_item) = <class 'dict'>, weather_item = {'id': 701, 'main': 'Mist', 'description': 'mist', 'icon': '50d'}
                    for weather_item in forecast_item.get('weather', [{}]) if forecast_item.get('weather') else {}:
                        # print(f"{type(weather_item) = }, {weather_item = }")
                        weather_condition_data = {
                            'condition_id': weather_item.get('id', 800),  # Default to clear sky
                            'condition_main': weather_item.get('main', 'Unknown'),
                            'description': weather_item.get('description', 'No description available'),
                            'icon_code': weather_item.get('icon', '01d'),
                            'is_precipitation': weather_item.get('main', '') in ['Rain', 'Snow', 'Drizzle']
                        }
                        # print(f"{weather_condition_data = }, {type(weather_condition_data) = }")
                        # weather_conditions.append(DimWeatherCondition.from_dict(weather_condition_data))
                        # Create individual DimWeatherCondition objects
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
                        # Add to the weather condition dimension object
                        weather_conditions.append(DimWeatherCondition.from_dict(default_weather_data))

                    print(f"{type(weather_conditions) = }, {weather_conditions = }")

                    # Get main forecast data with defaults
                    main_data = forecast_item.get('main', {})
                    wind_data = forecast_item.get('wind', {})
                    clouds_data = forecast_item.get('clouds', {})

                    # Create a WeatherDataDTO from the JSON data
                    weather_dto = WeatherDataDTO(
                        city_id=city.city_id,
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
                        # weather_condition_id=weather_data.get('id', 800),
                        weather_condition_ids=[condition.condition_id for condition in weather_conditions],
                        calculation_time=safe_datetime(forecast_item.get('dt', int(datetime.now().timestamp())))
                    )

                    # Determine precipitation type and volume if present
                    precipitation_type = PrecipitationType.NONE
                    precipitation_volume_3h = None

                    try:
                        if 'rain' in forecast_item:
                            precipitation_type = PrecipitationType.RAIN
                            precipitation_volume_3h = safe_decimal(forecast_item['rain'].get('3h', 0))
                        elif 'snow' in forecast_item:
                            precipitation_type = PrecipitationType.SNOW
                            precipitation_volume_3h = safe_decimal(forecast_item['snow'].get('3h', 0))
                    except Exception as e:
                        print(f"Error processing precipitation data: {e}")

                    weather_dto.precipitation_type = precipitation_type
                    weather_dto.precipitation_volume_3h = precipitation_volume_3h

                    # print(f"\n{weather_dto = }, {type(weather_dto) = }")

                    # Use the WeatherMapper to create dimension objects
                    # try:
                    dimensions = WeatherMapper.dto_to_dimensions(weather_dto)
                    # print("Successfully created dimension objects from DTO")
                    # print(f"{dimensions = }, {type(dimensions) = }")
                    # except Exception as e:
                    #    print(f"Failed to map DTO to dimensions: {e}")
                    #    raise

                    # Create date and time dimensions
                    forecast_datetime = safe_datetime(forecast_item.get('dt', int(datetime.now().timestamp())))

                    try:
                        date_dim = DateDimensionFactory.create_from_date(forecast_datetime.date())
                        time_dim = TimeDimensionFactory.create_from_time(forecast_datetime.time())
                    except Exception as e:
                        print(f"Failed to create date/time dimensions: {e}")
                        raise

                    # Extract probability of precipitation if available
                    pop = forecast_item.get('pop', 0) * 100  # Convert from 0-1 scale to percentage

                    forecast_fact = FactForecast(
                        forecast_item_id=idx + 1,  # This would be assigned by a database
                        forecast_id=1,  # This would be a group ID for all items in this forecast
                        city_id=city.city_id,
                        condition_ids=[condition.condition_id for condition in weather_conditions],
                        wind_id=dimensions['wind'].wind_id,
                        temperature_id=dimensions['temperature'].temperature_id,
                        pressure_id=dimensions['pressure'].pressure_id,
                        humidity_id=dimensions['humidity'].humidity_id,
                        precipitation_id=dimensions['precipitation'].precipitation_id,
                        date_id=date_dim.date_id,
                        time_id=time_dim.time_id,
                        forecast_time=forecast_datetime,
                        cloudiness=clouds_data.get('all', 0),
                        visibility=forecast_item.get('visibility', 10000),
                        probability_of_precipitation=pop,
                        created_at=datetime.now()
                    )
                    # print("Successfully created forecast fact object")
                    # print(f"{forecast_fact = }, {type(forecast_fact) = }")

                    forecast_results.append({
                        'weather_condition': weather_conditions,
                        'dimensions': dimensions,
                        'date_dim': date_dim,
                        'time_dim': time_dim,
                        'forecast_fact': forecast_fact
                    })
                except Exception as e:
                    print(f"Error processing forecast item {idx}: {e}")
                    continue

            print("\n\nSuccessfully created forecast_results object")
            print(f"{forecast_results = }, {type(forecast_results) = }")

        except KeyError as e:
            print(f"Missing required key in data: {e}")
            return None
        except Exception as e:
            print(f"Error processing forecast data: {e}")
            return None

    except Exception as e:
        print(f"Unexpected error in process_current_weather: {e}")
        return None

    return forecast_results


def main() -> None:
    """

    :param city_name:
    :param state_code:
    :param country_code:
    :param limit:
    :return:
    """
    try:
        print("Processing weather data...")
        # Process current weather
        # process_current_weather()
        # file_path = f"{output_files}forcast_{location}_{formatted_datetime}.json"
        # Process forecast
        process_forecast()

    except Exception as e:
        print(f"Unexpected error: {e}")


if __name__ == '__main__':
    main()
    # main('Toronto', '', country_code='CA')
    # main('Dublin', '', country_code = 'IE')
    # main('London', '', country_code='GB')
    # main('Solon', 'OH', 'US')
    # main('Letlhakane', '', country_code='BW')
