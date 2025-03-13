"""
This module contains domain models representing the fact tables for the weather system.
These are the central entities that connect dimensions and store measurements.
"""
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional, Dict, Any

from .atmospheric import Pressure, Humidity
# Import domain models from previously created models
from .common import Entity
from .location import City
from .weather import WeatherCondition, Wind, Temperature, Precipitation


# pylint: disable=too-many-instance-attributes
@dataclass
class FactCurrentWeather(Entity):
    """
    Domain entity representing current weather data for a specific location.
    Acts as a fact table connecting all dimension entities with current measurements.
    """
    city: City = field(default_factory=City)
    weather_condition: WeatherCondition = field(default_factory=WeatherCondition)
    temperature: Temperature = field(default_factory=Temperature)
    wind: Wind = field(default_factory=Wind)
    pressure: Pressure = field(default_factory=Pressure)
    humidity: Humidity = field(default_factory=Humidity)
    precipitation: Optional[Precipitation] = field(default_factory=Precipitation)
    clouds_percentage: Optional[int] = field(default_factory=int)
    visibility: Optional[int] = field(default_factory=int)
    feels_like_celsius: Optional[float] = field(default_factory=float)
    sunrise: Optional[datetime] = field(default_factory=datetime.now)
    sunset: Optional[datetime] = field(default_factory=datetime.now)
    timestamp: datetime = field(default_factory=datetime.now)
    timezone_offset: Optional[int] = field(default_factory=int)  # Offset from UTC in seconds
    data_source: str = "openweathermap"

    # Computed properties
    measurement_hour: int = field(init=False)
    is_daytime: bool = field(init=False)

    def __post_init__(self) -> None:
        """Calculate derived fields after initialization."""
        # Get the hour when measurement was taken
        self.measurement_hour = self.timestamp.hour

        # Determine if measurement was taken during daytime
        if self.sunrise and self.sunset:
            self.is_daytime = self.sunrise <= self.timestamp <= self.sunset
        else:
            # Default to day hours 6am-6pm if sunrise/sunset not available
            self.is_daytime = 6 <= self.measurement_hour < 18

    @property
    def local_time(self) -> datetime:
        """Get the local time based on the timezone offset."""
        if self.timezone_offset is not None:
            return self.timestamp + timedelta(seconds=self.timezone_offset)
        return self.timestamp

    @property
    def is_extreme_weather(self) -> bool:
        """Check if current weather conditions are extreme."""
        return self.weather_condition.is_extreme

    @property
    def weather_summary(self) -> str:
        """Provide a concise summary of current weather conditions."""
        temp_str = f"{float(self.temperature.temperature):.1f}°C"
        condition_str = self.weather_condition.description

        summary = f"{condition_str}, {temp_str}"

        # Add humidity if unusual
        if self.humidity.humidity < 30:
            summary += f", very dry ({self.humidity.humidity}%)"
        elif self.humidity.humidity > 80:
            summary += f", very humid ({self.humidity.humidity}%)"

        # Add wind if significant
        if float(self.wind.speed) > 8:
            summary += f", windy ({float(self.wind.speed):.1f} m/s)"

        # Add special alerts for extreme conditions
        if self.is_extreme_weather:
            summary = f"ALERT - Extreme Weather: {summary}"

        return summary

    @classmethod
    def from_api_response(cls, api_data: Dict[str, Any], city: City) -> 'FactCurrentWeather':
        """
        Factory method to create a FactCurrentWeather instance from a raw API response.

        Args:
            api_data: Raw data from the OpenWeatherMap API
            city: City entity for the location

        Returns:
            A new FactCurrentWeather instance
        """
        # This would be implemented based on the specific API format
        # Here we would extract and map all the needed fields from api_data
        # Create all required domain entities (WeatherCondition, Temperature, etc.)
        # Then construct and return the FactCurrentWeather instance

        # This is just a placeholder - implementation would depend on actual API format
        raise (NotImplementedError
               ("This method needs to be implemented for your specific API schema"))

    # pylint: disable=too-many-locals
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'FactCurrentWeather':
        """Create a FactCurrentWeather instance from a dictionary."""
        # Extract nested objects
        city_data = data.pop('city', {})
        city = city_data if isinstance(city_data, City) else City.from_dict(city_data)

        weather_condition_data = data.pop('weather_condition', {})
        weather_condition = (weather_condition_data
                             if isinstance(weather_condition_data, WeatherCondition)
                             else WeatherCondition.from_dict(weather_condition_data))

        temperature_data = data.pop('temperature', {})
        temperature = (temperature_data if isinstance(temperature_data, Temperature)
                       else Temperature.from_dict(temperature_data))

        wind_data = data.pop('wind', {})
        wind = wind_data if isinstance(wind_data, Wind) else Wind.from_dict(wind_data)

        pressure_data = data.pop('pressure', {})
        pressure = pressure_data if isinstance(pressure_data, Pressure) \
            else Pressure.from_dict(pressure_data)

        humidity_data = data.pop('humidity', {})
        humidity = humidity_data if isinstance(humidity_data, Humidity) \
            else Humidity.from_dict(humidity_data)

        precipitation_data = data.pop('precipitation', None)
        precipitation = None
        if precipitation_data:
            precipitation = (precipitation_data if isinstance(precipitation_data, Precipitation)
                             else Precipitation.from_dict(precipitation_data))

        # Process datetime fields
        for dt_field in ['timestamp', 'sunrise', 'sunset']:
            if dt_field in data and isinstance(data[dt_field], str):
                data[dt_field] = datetime.fromisoformat(data[dt_field])

        # Create instance with the extracted data
        return cls.create(
            city=city,
            weather_condition=weather_condition,
            temperature=temperature,
            wind=wind,
            pressure=pressure,
            humidity=humidity,
            precipitation=precipitation,
            **data
        )
