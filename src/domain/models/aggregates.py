"""
This module contains the domain models for the WeatherData aggregate root
following DDD principles.
The domain models represent core business concepts and encapsulate both data and behavior.
"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, Dict, Any

from .atmospheric import Pressure, Humidity
from .common import Entity
from .enums import HumidityCategory
from .location import City
from .weather import WeatherCondition, Temperature, Wind, Precipitation


# ------------------
# Aggregate Root
# ------------------

# pylint: disable=too-many-instance-attributes
@dataclass
class WeatherData(Entity):
    """Aggregate root representing complete weather data for a location at a specific time."""
    city: City = field(default_factory=City)
    timestamp: datetime = field(default_factory=datetime.now)
    weather_condition: WeatherCondition = field(default_factory=WeatherCondition)
    temperature: Temperature = field(default_factory=Temperature)
    humidity: Humidity = field(default_factory=Humidity)
    pressure: Pressure = field(default_factory=Pressure)
    wind: Wind = field(default_factory=Wind)
    precipitation: Optional[Precipitation] = field(default=None)
    clouds_percentage: Optional[int] = field(default_factory=int)
    visibility_meters: Optional[int] = field(default_factory=int)
    data_source: Optional[str] = field(default_factory=str)

    @property
    def is_extreme_weather(self) -> bool:
        """Check if the weather conditions are extreme."""
        return self.weather_condition.is_extreme

    @property
    def feels_like_description(self) -> str:
        """Get a description of how the weather feels."""
        # Temperature feel
        temp_feel = f"{getattr(self.temperature.comfort_level, 'value', 'N/A')}"

        # Add humidity impact if significant
        if self.humidity.humidity_category == HumidityCategory.DRY and \
                float(self.temperature.temperature) > 25:
            return f"{temp_feel}, but dry"
        if self.humidity.humidity_category in \
                [HumidityCategory.HUMID, HumidityCategory.VERY_HUMID]:
            if float(self.temperature.temperature) > 20:
                return f"{temp_feel} and humid, feels muggy"

        # Add wind impact if significant
        if float(self.wind.speed) > 8:
            # wind_impact = "windy"
            if float(self.temperature.temperature) < 10:
                return f"{temp_feel} with significant wind chill"

        return temp_feel

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'WeatherData':
        """Create a WeatherData instance from a dictionary with nested objects."""
        data_copy = data.copy()

        # Extract and convert nested objects
        nested_objects = {}
        for key, class_type in {
            'city': City,
            'weather_condition': WeatherCondition,
            'temperature': Temperature,
            'humidity': Humidity,
            'pressure': Pressure,
            'wind': Wind
        }.items():
            obj_data = data_copy.pop(key, {})
            nested_objects[key] = (obj_data if isinstance(obj_data, class_type)
                                   else class_type.from_dict(obj_data))  # type: ignore

            # Handle precipitation separately since it can be None
            precipitation_data: Optional[Dict[str, Any]] = data_copy.pop('precipitation', None)
            if precipitation_data:
                nested_objects['precipitation'] = (precipitation_data
                                                   if isinstance(precipitation_data, Precipitation)
                                                   else Precipitation.from_dict(precipitation_data))
            else:
                nested_objects['precipitation'] = None

        # Handle timestamp
        if 'timestamp' in data_copy and isinstance(data_copy['timestamp'], str):
            data_copy['timestamp'] = datetime.fromisoformat(data_copy['timestamp'])

        # Create the WeatherData instance
        return cls.create(**nested_objects, **data_copy)

    @classmethod
    def create_from_api_response(cls, api_data: Dict[str, Any], city: City) -> 'WeatherData':
        """Factory method to create WeatherData from an API response."""
        # This method would be implemented to handle parsing of specific API formats
        # like OpenWeatherMap responses
        raise NotImplementedError("This method needs to be implemented based on the specific"
                                  "API response format")
