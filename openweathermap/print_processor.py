"""
This python script prints current weather and forecast data from OpenWeather API to stdout.
"""
from typing import Union, List, Any, Dict

from openweathermap.datasets.weather import (DimPrecipitation,
                                             DimHumidity, DimTemperature, DimWeatherCondition)


def print_current_weather_results(result: Dict) -> None:
    """

    :param result:
    """
    # Extract data for display
    weather_condition = result['weather_condition']
    dimensions = result['dimensions']
    fact = result['fact']

    print("\nCurrent Weather Results:")
    print(f"Source System: {fact.source_system}")
    print(f"Location: {fact.city_id}")
    print(f"Date/Time: {fact.calculation_time.strftime('%m/%d/%Y %I:%M:%S %p')}")
    print_temperature(dimensions['temperature'])
    print_weather_conditions(weather_condition)
    print(f"Wind: {dimensions['wind'].speed} m/s"
          f" from {dimensions['wind'].direction_cardinal}"
          f"   Gust: {dimensions['wind'].gust_speed} m/s"
          f" - {dimensions['wind'].beaufort_scale}")
    print(f"Pressure: {dimensions['pressure'].pressure} hPa"
          f"    Pressure Category: {dimensions['pressure'].pressure_category}"
          f"    Storm Potential - {dimensions['pressure'].storm_potential}")
    print_humidity(dimensions['humidity'])
    print(f"Cloudiness: {fact.cloudiness}%")
    print(f"Visibility: {fact.visibility} meters")
    print(f"{type(dimensions['precipitation']) = }")
    print_precipitation(dimensions['precipitation'])
    print("Processing completed successfully")


def print_forecast_results(results: list[dict[str, Any]] | None) -> None:
    """

    :param result:
    """
    if results is None:
        return

    print(f"\n\n{results = }")
    # Display results for the first few forecast items
    print("\nForecast Results:")

    # Limit to 3 items for display
    display_count = min(3, len(results))

    for i, result in enumerate(results[:display_count]):
        weather_condition = result['weather_condition']
        dimensions = result['dimensions']
        fact = result['fact']

        print(f"\nForecast {i + 1} - {fact.forecast_time.strftime('%m/%d/%Y %I:%M %p')}:")
        print(f"Source System: {fact.source_system}")
        print(f"Location: {fact.city_id}")
        print_temperature(dimensions['temperature'])
        print_weather_conditions(weather_condition)
        print(f"Wind: {dimensions['wind'].speed} m/s"
              f" from {dimensions['wind'].direction_cardinal}"
              f"   Gust: {dimensions['wind'].gust_speed} m/s"
              f" - {dimensions['wind'].beaufort_scale}")
        print_humidity(dimensions['humidity'])
        print(f"Cloudiness: {fact.cloudiness}%")
        print(f"Visibility: {fact.visibility} meters")
        print(f"Probability of Precipitation: {fact.probability_of_precipitation}%")
        print_precipitation(dimensions['precipitation'])

    print(f"\nTotal forecast periods processed: {len(results)}")
    print("Processing completed successfully")


def print_weather_conditions(weather_condition: Union[List[DimWeatherCondition],
DimWeatherCondition, Any]) -> None:
    """
    Print weather conditions in a formatted way.

    Args:
        weather_condition: Either a list of weather conditions, a single weather
        condition object or any other type (which will be handled safely)
    """
    if isinstance(weather_condition, list):
        condition_text = ', '.join([
            f"{cond.condition_id}: {cond.description} (icon: {cond.icon_code})"
            for cond in weather_condition
        ])
    elif hasattr(weather_condition, 'description'):
        # Single weather condition object
        condition_text = (f"{weather_condition.condition_id}:"
                          f"{weather_condition.description} (icon: {weather_condition.icon_code})")
    else:
        # Fallback for unexpected types
        condition_text = str(weather_condition)

    print(f"Weather Conditions: {condition_text}")


def print_temperature(temperature: DimTemperature) -> None:
    """

    :param temperature:
    """
    print(f"Temperature: {temperature.temperature}°F (feels like"
          f" {temperature.feels_like}°F)")
    print(f"Minimum Temperature: {temperature.temp_min}°F"
          f" Maximum Temperature: {temperature.temp_max}°F")
    print(f"Comfort Level: {temperature.comfort_level}"
          f"   Temperature Category: {temperature.temperature_category}")


def print_humidity(humidity: DimHumidity) -> None:
    """

    :param humidity:
    """
    dew_point_risk = ("Yes" if humidity.is_dew_point_risk is True else "No")
    print(f"Humidity: {humidity.humidity}%"
          f"    Humidity Category: {humidity.humidity_category}"
          f"    Comfort Level: {humidity.comfort_impact}"
          f"    Dew Point Risk: {dew_point_risk}"
          f"    Mold Risk Level: {humidity.mold_risk_level}")


def print_precipitation(precip: DimPrecipitation) -> None:
    """

    :param dimensions:
    """
    volume_1h = (f"{precip.volume_1h}"
                 if precip.volume_1h is not None else "0.0")
    volume_3h = (f"{precip.volume_3h}"
                 if precip.volume_3h is not None else "0.0")
    impact_level = (f"{precip.impact_level.capitalize()}"
                    if precip.impact_level is not None else "Unknown")
    flooding_risk = (f"{precip.flooding_risk.capitalize()}"
                     if precip.flooding_risk is not None else "Unknown")
    print(f"Precipitation: {precip.precipitation_type}"
          f"    1 Hour precipitation: {volume_1h} mm"
          f"    3 Hour precipitation: {volume_3h} mm"
          f"    Intensity: {precip.intensity_category}"
          f"    Impact Level: {impact_level}"
          f"    Flooding Risk: {flooding_risk}")
