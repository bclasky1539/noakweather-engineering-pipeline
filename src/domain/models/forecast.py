"""
This module contains domain models representing the fact tables for the weather system.
These are the central entities that connect dimensions and store measurements.
"""
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from typing import Optional, List, Dict, Any

from .atmospheric import Pressure, Humidity
# Import domain models from previously created models
from .common import Entity
from .location import City
from .weather import WeatherCondition, Wind, Temperature, Precipitation


# pylint: disable=too-many-instance-attributes
@dataclass
class ForecastItem(Entity):
    """
    Domain entity representing a single forecast item for a specific time point.
    """
    timestamp: datetime = field(default_factory=datetime.now)
    weather_condition: WeatherCondition = field(default_factory=WeatherCondition)
    temperature: Temperature = field(default_factory=Temperature)
    wind: Wind = field(default_factory=Wind)
    pressure: Pressure = field(default_factory=Pressure)
    humidity: Humidity = field(default_factory=Humidity)
    precipitation: Optional[Precipitation] = field(default_factory=Precipitation)
    clouds_percentage: Optional[int] = field(default_factory=int)
    visibility_meters: Optional[int] = field(default_factory=int)
    forecast_confidence: Optional[float] = field(default_factory=float)
    pop: Optional[float] = field(default_factory=float)  # Probability of precipitation (0-1)

    @property
    def is_extreme_weather(self) -> bool:
        """Check if forecasted weather conditions are extreme."""
        return self.weather_condition.is_extreme

    @property
    def forecast_summary(self) -> str:
        """Provide a concise summary of forecasted conditions."""
        time_str = self.timestamp.strftime("%a %H:%M")
        temp_str = f"{float(self.temperature.temperature):.1f}°C"
        condition_str = self.weather_condition.description

        summary = f"{time_str}: {condition_str}, {temp_str}"

        # Add precipitation probability if available
        if self.pop is not None and self.pop > 0.2:
            summary += f", {int(self.pop * 100)}% chance of precipitation"

        # Add wind if significant
        if float(self.wind.speed) > 8:
            summary += f", windy ({float(self.wind.speed):.1f} m/s)"

        # Add special alerts for extreme conditions
        if self.is_extreme_weather:
            summary = f"ALERT - {summary}"

        return summary

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'ForecastItem':
        """Create a ForecastItem instance from a dictionary."""
        # Similar implementation to FactCurrentWeather.from_dict
        # Would extract and convert all the nested entities

        # For brevity, implementation details are omitted but would be similar
        # to the from_dict method in FactCurrentWeather

        # Process datetime fields
        if 'timestamp' in data and isinstance(data['timestamp'], str):
            data['timestamp'] = datetime.fromisoformat(data['timestamp'])

        # Create instance
        return cls.create(**data)


@dataclass
class FactForecast(Entity):
    """
    Domain entity representing a complete weather forecast for a location.
    Contains a collection of forecast items for different time points.
    """
    city: City = field(default_factory=City)
    forecast_items: List[ForecastItem] = field(default_factory=list)
    timestamp: datetime = field(default_factory=datetime.now)  # When the forecast was generated
    forecast_count: int = field(default_factory=int)
    data_source: str = field(default='openweathermap')
    # Type of forecast (e.g., 5day/3hour, daily, etc.)
    forecast_type: str = field(default='5day/3hour')

    def __post_init__(self) -> None:
        """Calculate derived fields after initialization."""
        self.forecast_count = len(self.forecast_items)

    def get_forecast_for_day(self, day_date: date) -> List[ForecastItem]:
        """Get all forecast items for a specific day."""
        return [
            item for item in self.forecast_items
            if item.timestamp.date() == day_date
        ]

    def get_daytime_forecasts(self) -> List[ForecastItem]:
        """Get forecasts for daytime hours (6 AM - 8 PM)."""
        return [
            item for item in self.forecast_items
            if 6 <= item.timestamp.hour < 20
        ]

    def get_extreme_weather_forecasts(self) -> List[ForecastItem]:
        """Get forecasts predicting extreme weather conditions."""
        return [
            item for item in self.forecast_items
            if item.is_extreme_weather
        ]

    def has_precipitation_upcoming(self, hours: int = 24) -> bool:
        """Check if any precipitation is expected in the upcoming hours."""
        cutoff_time = self.timestamp + timedelta(hours=hours)

        for item in self.forecast_items:
            if item.timestamp > cutoff_time:
                break

            if (item.precipitation and
                    item.precipitation.precipitation_type != "none" and
                    (item.precipitation.volume_1h or item.precipitation.volume_3h)):
                return True

            if item.pop and item.pop > 0.5:  # More than 50% chance of precipitation
                return True

        return False

    @property
    def daily_summaries(self) -> Dict[date, str]:
        """Generate daily summary for each forecasted day."""
        daily_items: dict[date, list[Any]] = {}

        for item in self.forecast_items:
            day = item.timestamp.date()
            if day not in daily_items:
                daily_items[day] = []
            daily_items[day].append(item)

        summaries = {}
        for day, items in daily_items.items():
            # Get min and max temperatures
            temps = [float(item.temperature.temperature) for item in items]
            min_temp = min(temps)
            max_temp = max(temps)

            # Get most common weather condition
            conditions: dict[Any, int | Any] = {}
            for item in items:
                cond = item.weather_condition.condition_main
                conditions[cond] = conditions.get(cond, 0) + 1

            common_condition = max(conditions.items(), key=lambda x: x[1])[0]

            # Create summary
            day_str = day.strftime("%A, %b %d")
            summaries[day] = f"{day_str}: {common_condition}, {min_temp:.1f}°C to {max_temp:.1f}°C"

            # Add precipitation info if relevant
            has_rain = any(
                item.precipitation and
                item.precipitation.precipitation_type == "rain" and
                (item.precipitation.volume_1h or item.precipitation.volume_3h)
                for item in items
            )

            if has_rain:
                summaries[day] += ", expect rain"

            # Add extreme weather warning
            has_extreme = any(item.is_extreme_weather for item in items)
            if has_extreme:
                summaries[day] = f"ALERT - {summaries[day]}"

        return summaries

    @classmethod
    def from_api_response(cls, api_data: Dict[str, Any], city: City) -> 'FactForecast':
        """
        Factory method to create a FactForecast instance from a raw API response.

        Args:
            api_data: Raw data from the OpenWeatherMap API
            city: City entity for the location

        Returns:
            A new FactForecast instance
        """
        # Implementation would depend on the specific API format
        # Would process the list of forecast items from api_data
        # Convert them to ForecastItem instances
        # Then construct and return the FactForecast

        # This is just a placeholder
        raise NotImplementedError(
            "This method needs to be implemented for your specific API schema")

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'FactForecast':
        """Create a FactForecast instance from a dictionary."""
        data_copy = data.copy()

        # Extract city
        city_data = data_copy.pop('city', {})
        city = city_data if isinstance(city_data, City) else City.from_dict(city_data)

        # Extract and convert forecast items
        forecast_items_data = data_copy.pop('forecast_items', [])
        forecast_items = []

        for item_data in forecast_items_data:
            if isinstance(item_data, ForecastItem):
                forecast_items.append(item_data)
            else:
                forecast_items.append(ForecastItem.from_dict(item_data))

        # Process timestamp
        if 'timestamp' in data_copy and isinstance(data_copy['timestamp'], str):
            data_copy['timestamp'] = datetime.fromisoformat(data_copy['timestamp'])

        # Create instance
        return cls.create(
            city=city,
            forecast_items=forecast_items,
            **data_copy
        )
