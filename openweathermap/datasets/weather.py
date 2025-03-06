"""
This python script is for the shared dataclasses used for the current weather
and forecast for the openweather API.
"""
import decimal
from bisect import bisect_right
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from decimal import Decimal
from enum import Enum
from typing import Optional, List, Tuple, Dict, Self, Any

from utils.property_conversions import temp_unit_conversion


# Enum types matching database enums
class PrecipitationType(str, Enum):
    """
    Precipitation types supported by OpenWeatherMap.
    """
    RAIN = "rain"
    SNOW = "snow"
    SLEET = "sleet"
    HAIL = "hail"
    NONE = "none"


class WindDirection(str, Enum):
    """
    Wind directions supported by OpenWeatherMap.
    """
    N = "N"
    NNE = "NNE"
    NE = "NE"
    ENE = "ENE"
    E = "E"
    ESE = "ESE"
    SE = "SE"
    SSE = "SSE"
    S = "S"
    SSW = "SSW"
    SW = "SW"
    WSW = "WSW"
    W = "W"
    WNW = "WNW"
    NW = "NW"
    NNW = "NNW"


class BeaufortCategory(Enum):
    """
    Beaufort categories supported by OpenWeatherMap.
    """
    CALM = "Calm"
    LIGHT_AIR = "Light Air"
    LIGHT_BREEZE = "Light Breeze"
    GENTLE_BREEZE = "Gentle Breeze"
    MODERATE_BREEZE = "Moderate Breeze"
    FRESH_BREEZE = "Fresh Breeze"
    STRONG_BREEZE = "Strong Breeze"
    NEAR_GALE = "Near Gale"
    GALE = "Gale"
    STRONG_GALE = "Strong Gale"
    STORM = "Storm"
    VIOLENT_STORM = "Violent Storm"
    HURRICANE = "Hurricane"

    def __init__(self, scale: int):  # , description: str):
        self.scale = scale
        # self.description = description

    @classmethod
    def get_boundaries(cls) -> List[Tuple[float, 'BeaufortCategory']]:
        """Return the upper boundaries of each Beaufort category."""
        return [
            (0.5, cls.CALM),
            (1.5, cls.LIGHT_AIR),
            (3.3, cls.LIGHT_BREEZE),
            (5.5, cls.GENTLE_BREEZE),
            (7.9, cls.MODERATE_BREEZE),
            (10.7, cls.FRESH_BREEZE),
            (13.8, cls.STRONG_BREEZE),
            (17.1, cls.NEAR_GALE),
            (20.7, cls.GALE),
            (24.4, cls.STRONG_GALE),
            (28.4, cls.STORM),
            (32.6, cls.VIOLENT_STORM),
            (float('inf'), cls.HURRICANE)
        ]

    @classmethod
    def wind_force_cat(cls, speed: float) -> 'BeaufortCategory':
        """Get the Beaufort category for the given wind speed."""
        boundaries = [0.5, 1.5, 3.3, 5.5, 7.9, 10.7, 13.8, 17.1, 20.7, 24.4, 28.4, 32.6]
        categories = [
            cls.CALM, cls.LIGHT_AIR, cls.LIGHT_BREEZE, cls.GENTLE_BREEZE,
            cls.MODERATE_BREEZE, cls.FRESH_BREEZE, cls.STRONG_BREEZE, cls.NEAR_GALE,
            cls.GALE, cls.STRONG_GALE, cls.STORM, cls.VIOLENT_STORM, cls.HURRICANE
        ]

        index = bisect_right(boundaries, speed)
        return categories[index]

    @classmethod
    def beaufort_scale(cls, speed: float) -> int:
        """Get the Beaufort category for the given wind speed."""
        boundaries = [0.5, 1.5, 3.3, 5.5, 7.9, 10.7, 13.8, 17.1, 20.7, 24.4, 28.4, 32.6,
                      float('inf')]
        categories = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]

        index = bisect_right(boundaries, speed)
        return categories[index]


class ComfortLevel(str, Enum):
    """
    Comfort levels supported by OpenWeatherMap.
    """
    VERY_COLD = "Very Cold"
    COLD = "Cold"
    COOL = "Cool"
    COMFORTABLE = "Comfortable"
    WARM = "Warm"
    HOT = "Hot"
    VERY_HOT = "Very Hot"

    @classmethod
    def comfort_level(cls, temperature: float, unit: str = 'C') -> 'ComfortLevel':
        """Get the Comfort Level for the given temperature."""
        temp_celsius = temp_unit_conversion(temperature, unit)

        boundaries = [-10.0, 0.0, 10.0, 18.0, 24.0, 29.0, 35.0]
        categories = [
            cls.VERY_COLD, cls.COLD, cls.COOL, cls.COMFORTABLE,
            cls.WARM, cls.HOT, cls.VERY_HOT
        ]

        index = bisect_right(boundaries, temp_celsius)
        return categories[index]


class TemperatureCategory(str, Enum):
    """
    Temperature Category.
    """
    EXTREME_COLD = "Extreme Cold"
    VERY_COLD = "Very Cold"
    FREEZING = "Freezing"
    COLD = "Cold"
    COOL = "Cool"
    MILD = "Mild"
    WARM = "Warm"
    HOT = "Hot"
    VERY_HOT = "Extreme Hot"

    @classmethod
    def temperature_category(cls, temperature: float, unit: str = 'C') -> 'TemperatureCategory':
        """Calculate temperature category."""
        temp_celsius = temp_unit_conversion(temperature, unit)

        boundaries = [-20.0, -10.0, 0.0, 10.0, 18.0, 24.0, 29.0, 35.0]
        categories = [
            cls.EXTREME_COLD, cls.VERY_COLD, cls.FREEZING, cls.COLD, cls.COOL,
            cls.MILD, cls.WARM, cls.HOT, cls.VERY_HOT
        ]

        index = bisect_right(boundaries, temp_celsius)
        return categories[index]


class PressureTrend(str, Enum):
    """
    Pressure trends supported by OpenWeatherMap.
    """
    FALLING = "falling"
    STABLE = "stable"
    RISING = "rising"
    UNKNOWN = "unknown"


class HumidityCategory(str, Enum):
    """
    Humidity categories supported by OpenWeatherMap.
    """
    DRY = "dry"
    COMFORTABLE = "comfortable"
    HUMID = "humid"
    VERY_HUMID = "very_humid"

    @classmethod
    def humidity_category(cls, humidity: int) -> 'HumidityCategory':
        """Calculate temperature category."""
        boundaries = [30, 50, 70, 100]
        categories = [
            cls.DRY, cls.COMFORTABLE, cls.HUMID, cls.VERY_HUMID
        ]

        index = bisect_right(boundaries, humidity)
        return categories[index]


class IntensityCategory(str, Enum):
    """
    Intensity categories supported by OpenWeatherMap.
    """
    NONE = "none"
    VERY_LIGHT = "very_light"
    LIGHT = "light"
    MODERATE = "moderate"
    HEAVY = "heavy"
    EXTREME = "extreme"

    @classmethod
    def intensity_category(cls, vol: float) -> 'IntensityCategory':
        """Calculate temperature category."""
        boundaries = [0.0, 10.0, 30.0, 50.0, 70.0, float('inf')]
        categories = [
            cls.NONE, cls.VERY_LIGHT, cls.LIGHT, cls.MODERATE,
            cls.HEAVY, cls.EXTREME
        ]

        index = bisect_right(boundaries, vol)
        return categories[index]


# Base class for common fields
@dataclass
class BaseDimension:
    """
    Base dimension dataclass.
    """
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: Optional[datetime] = field(default=None)


# This implementation:
#
# Adds a new local_names dictionary field to store translations indexed by language code
# Updates both from_dict methods to handle the local_names field from the input data
# Adds a new get_name() method to retrieve the name in a specific language (with fallback)
#
# You could use the class like this:
# # Create a city with multiple language names
# london_data = {
#     "city_id": 2643743,
#     "name": "London",
#     "local_names": {
#         "en": "London",
#         "fr": "Londres",
#         "es": "Londres",
#         "de": "London",
#         "ru": "Лондон",
#         "ja": "ロンドン"
#     },
#     "country": "GB",
#     "timezone": 0,
#     "latitude": 51.5074,
#     "longitude": -0.1278
# }
#
# london = DimCity.from_dict(london_data)
#
# # Get name in different languages
# print(london.get_name())      # "London" (default)
# print(london.get_name("fr"))  # "Londres"
# print(london.get_name("ru"))  # "Лондон"
# print(london.get_name("zh"))  # "London" (falls back to default)
# This approach gives you a flexible way to store and access localized city
# names while maintaining compatibility with the rest of your data model.
@dataclass
class DimCity(BaseDimension):
    """
    DimCity dataclass.
    """
    city_id: int = field(default=0)
    name: str = field(default="")
    local_names: Dict[str, str] = field(default_factory=dict)  # Store names by language code
    country: str = field(default="")
    timezone: int = field(default=0)
    latitude: Decimal = field(default=Decimal('0.0'))
    longitude: Decimal = field(default=Decimal('0.0'))
    state: Optional[str] = field(default=None)
    population: Optional[int] = field(default=None)
    sunrise: Optional[datetime] = field(default=None)
    sunset: Optional[datetime] = field(default=None)
    elevation_meters: Optional[int] = field(default=None)
    region: Optional[str] = field(default=None)

    # This implementation:
    #
    # Uses @classmethod to create a factory method that can be inherited properly
    # Creates a copy of the input dictionary to avoid modifying the original
    # Handles conversion of:
    #
    # String values to Decimal for latitude and longitude
    # String values to datetime for sunrise and sunset
    # Various types to int for population and elevation_meters
    #
    #
    # Uses the class reference to instantiate a new object, which makes it work with inheritance
    #
    # The method handles the various data types in your class, including the Decimal type for
    # coordinates and Optional fields that might be None.
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimCity instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimCity attributes.

        Returns:
            DimCity: A new DimCity instance.
        """
        data_copy = data.copy()

        # Handle local_names if present, expecting format like:
        # {"local_names": {"en": "London", "fr": "Londres", "de": "London"}}
        local_names = {}
        if 'local_names' in data_copy:
            if isinstance(data_copy['local_names'], dict):
                local_names = data_copy['local_names']
            # Remove from data_copy to avoid duplicate keyword argument
            del data_copy['local_names']

        # Convert string to Decimal for latitude and longitude
        for decimal_field in ['latitude', 'longitude']:
            if decimal_field in data_copy and not isinstance(data_copy[decimal_field], Decimal):
                data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Convert string to datetime for sunrise and sunset
        for datetime_field in ['sunrise', 'sunset']:
            if datetime_field in data_copy and isinstance(data_copy[datetime_field], str):
                data_copy[datetime_field] = datetime.fromisoformat(data_copy[datetime_field])

        # Ensure proper types for optional integer fields
        for int_field in ['population', 'elevation_meters']:
            if int_field in data_copy and data_copy[int_field] is not None:
                data_copy[int_field] = int(data_copy[int_field])

        # Add the local_names to the parameters
        return cls(local_names=local_names, **data_copy)

    # Key differences from the class method version:
    #
    # Uses the @staticmethod decorator instead of @classmethod
    # Does not have a cls parameter
    # Explicitly references DimCity in the return statement rather than using cls
    # If a subclass inherits this method, it will still return a DimCity instance, not an
    # instance of the subclass
    #
    # This approach works but has drawbacks compared to using a class method, particularly:
    #
    # It's less adaptable to inheritance
    # If the class name changes, you need to update the hardcoded class name in the return statement
    # It doesn't benefit from polymorphism (a subclass calling this method will get a parent class
    # instance, not a subclass instance)
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimCity":
        """
        Create a DimCity instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimCity attributes.

        Returns:
            DimCity: A new DimCity instance.
        """
        data_copy = data.copy()

        # Handle local_names if present
        local_names = {}
        if 'local_names' in data_copy:
            if isinstance(data_copy['local_names'], dict):
                local_names = data_copy['local_names']
            # Remove from data_copy to avoid duplicate keyword argument
            del data_copy['local_names']

        # Convert string to Decimal for latitude and longitude
        for decimal_field in ['latitude', 'longitude']:
            if decimal_field in data_copy and not isinstance(data_copy[decimal_field], Decimal):
                data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Convert string to datetime for sunrise and sunset
        for datetime_field in ['sunrise', 'sunset']:
            if datetime_field in data_copy and isinstance(data_copy[datetime_field], str):
                data_copy[datetime_field] = datetime.fromisoformat(data_copy[datetime_field])

        # Ensure proper types for optional integer fields
        for int_field in ['population', 'elevation_meters']:
            if int_field in data_copy and data_copy[int_field] is not None:
                data_copy[int_field] = int(data_copy[int_field])

        # Must explicitly reference the DimCity class here and add local_names
        return DimCity(local_names=local_names, **data_copy)

    def get_name(self, lang_code: str = '') -> str:
        """
        Get the city name in the specified language if available.
        Falls back to the default name if the requested language is not available.

        Args:
            lang_code (str, optional): The language code (e.g., 'en', 'fr'). Defaults to None.

        Returns:
            str: The city name in the requested language or the default name.
        """
        if lang_code and lang_code in self.local_names:
            return self.local_names[lang_code]
        return self.name

    @property
    def coordinates(self) -> tuple:
        """Return the point coordinates as a tuple."""
        return (float(self.longitude), float(self.latitude))

    @property
    def utc_offset(self) -> timedelta:
        """Calculate the timezone offset as a timedelta."""
        return timedelta(hours=self.timezone)


@dataclass
class DimWeatherCondition(BaseDimension):
    """
    Dimension class for the Weather condition.
    """
    condition_id: int = field(default=0)
    condition_main: str = field(default="")
    description: str = field(default="")
    icon_code: str = field(default="")
    severity_level: Optional[int] = field(default=None)
    is_precipitation: bool = field(default=False)
    is_extreme: bool = field(default=False)

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they weren't provided.
        :rtype: object
        """
        if self.is_extreme is False:
            self._is_extreme_weather()

    # TODO This needs proper testing # pylint: disable=fixme
    def _is_extreme_weather(self) -> None:
        """Determine if a weather condition is considered extreme."""
        extreme_ranges = [
            (202, 232),  # Severe thunderstorms
            (504, 531),  # Extreme rain events
            (602, 622),  # Heavy snow events
            (751, 781),  # Severe atmospheric conditions
            (900, 906),  # Extreme additional conditions
            (961, 962)  # Violent storms
        ]

        self.is_extreme = any(start <= self.condition_id <= end for start, end in extreme_ranges)

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimWeatherCondition instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimWeatherCondition attributes.

        Returns:
            DimWeatherCondition: A new DimWeatherCondition instance.
        """
        data_copy = data.copy()

        # Ensure proper type for condition_id
        if 'condition_id' in data_copy and not isinstance(data_copy['condition_id'], int):
            data_copy['condition_id'] = int(data_copy['condition_id'])

        # Ensure proper type for severity_level if present
        if 'severity_level' in data_copy and data_copy['severity_level'] is not None:
            data_copy['severity_level'] = int(data_copy['severity_level'])

        # Convert boolean fields from strings if needed
        for bool_field in ['is_precipitation', 'is_extreme']:
            if bool_field in data_copy and isinstance(data_copy[bool_field], str):
                data_copy[bool_field] = (data_copy[bool_field].lower()
                                         in ['true', 't', 'yes', 'y', '1'])

        return cls(**data_copy)

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimWeatherCondition":
        """
        Create a DimWeatherCondition instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimWeatherCondition attributes.

        Returns:
            DimWeatherCondition: A new DimWeatherCondition instance.
        """
        data_copy = data.copy()

        # Ensure proper type for condition_id
        if 'condition_id' in data_copy and not isinstance(data_copy['condition_id'], int):
            data_copy['condition_id'] = int(data_copy['condition_id'])

        # Ensure proper type for severity_level if present
        if 'severity_level' in data_copy and data_copy['severity_level'] is not None:
            data_copy['severity_level'] = int(data_copy['severity_level'])

        # Convert boolean fields from strings if needed
        for bool_field in ['is_precipitation', 'is_extreme']:
            if bool_field in data_copy and isinstance(data_copy[bool_field], str):
                data_copy[bool_field] = (data_copy[bool_field].lower()
                                         in ['true', 't', 'yes', 'y', '1'])

        # Must explicitly reference the DimWeatherCondition class here
        return DimWeatherCondition(**data_copy)


@dataclass
class DimWind(BaseDimension):
    """
    Dimension class for the Wind direction.
    """
    wind_id: int = field(default=0)
    speed: Decimal = field(default=Decimal('0.0'))
    degrees: int = field(default=0)
    gust_speed: Optional[Decimal] = field(default=Decimal('0.0'))
    direction_cardinal: Optional[str] = field(default=None)
    beaufort_scale: Optional[int] = field(default=-99)
    beaufort_description: Optional[str] = field(default=None)

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they weren't provided."""
        if self.direction_cardinal is None:
            self._direction_cardinal()
        if self.beaufort_scale == -99 or self.beaufort_description is None:
            self._wind_force_category()

    # TODO This needs proper testing # pylint: disable=fixme
    def _direction_cardinal(self) -> None:
        """Calculate the cardinal direction based on degrees."""
        # Convert degrees to an index from 0-15 (16 directions)
        index = int((self.degrees + 11.25) % 360 / 22.5)

        # Map the index to the appropriate WindDirection enum
        directions = [
            WindDirection.N, WindDirection.NNE, WindDirection.NE, WindDirection.ENE,
            WindDirection.E, WindDirection.ESE, WindDirection.SE, WindDirection.SSE,
            WindDirection.S, WindDirection.SSW, WindDirection.SW, WindDirection.WSW,
            WindDirection.W, WindDirection.WNW, WindDirection.NW, WindDirection.NNW
        ]
        self.direction_cardinal = directions[index]

    # TODO This needs proper testing # pylint: disable=fixme
    def _wind_force_category(self) -> None:
        """Calculate the Beaufort scale category."""
        # return strings as before but still use enums internally, you could modify
        # the function to return
        # BeaufortCategory.from_speed(float(self.speed)).value.
        # TODO This needs to be properly tested # pylint: disable=fixme
        category = BeaufortCategory.wind_force_cat(float(self.speed))
        self.beaufort_scale = category.scale
        # self.beaufort_description = category.description

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimWind instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimWind attributes.

        Returns:
            DimWind: A new DimWind instance.
        """
        # TODO Implement try/except in all areas # pylint: disable=fixme
        try:
            data_copy = data.copy()

            # Ensure proper type for wind_id
            if 'wind_id' in data_copy and not isinstance(data_copy['wind_id'], int):
                data_copy['wind_id'] = int(data_copy['wind_id'])

            # Ensure proper type for speed
            if 'speed' in data_copy and not isinstance(data_copy['speed'], Decimal):
                data_copy['speed'] = Decimal(str(data_copy['speed']))

            # Ensure proper type for degrees
            if 'degrees' in data_copy and not isinstance(data_copy['degrees'], int):
                data_copy['degrees'] = int(data_copy['degrees'])

            # Ensure proper type for gust_speed if present
            if ('gust_speed' in data_copy and data_copy['gust_speed']
                    is not None and not isinstance(data_copy['gust_speed'], Decimal)):
                data_copy['gust_speed'] = Decimal(str(data_copy['gust_speed']))

            return cls(**data_copy)
        except (ValueError, TypeError, decimal.InvalidOperation) as e:
            print(f"Error creating {cls.__name__} from data: {e}")
            raise ValueError(f"Invalid wind data: {e}")

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimWind":
        """
        Create a DimWind instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimWind attributes.

        Returns:
            DimWind: A new DimWind instance.
        """
        data_copy = data.copy()

        # Ensure proper type for wind_id
        if 'wind_id' in data_copy and not isinstance(data_copy['wind_id'], int):
            data_copy['wind_id'] = int(data_copy['wind_id'])

        # Ensure proper type for speed
        if 'speed' in data_copy and not isinstance(data_copy['speed'], Decimal):
            data_copy['speed'] = Decimal(str(data_copy['speed']))

        # Ensure proper type for degrees
        if 'degrees' in data_copy and not isinstance(data_copy['degrees'], int):
            data_copy['degrees'] = int(data_copy['degrees'])

        # Ensure proper type for gust_speed if present
        if ('gust_speed' in data_copy and data_copy['gust_speed']
                is not None and not isinstance(data_copy['gust_speed'], Decimal)):
            data_copy['gust_speed'] = Decimal(str(data_copy['gust_speed']))

        # Must explicitly reference the DimWind class here
        return DimWind(**data_copy)

    @property
    def is_gusty(self) -> bool:
        """Determine if wind is gusty."""
        if self.gust_speed is None:
            return False
        return float(self.gust_speed) > float(self.speed) * 1.3

    @property
    def _beaufort_scale(self) -> int:
        """Calculate the Beaufort scale number."""
        # return self.beaufort_category().scale
        return BeaufortCategory.beaufort_scale(float(self.speed))


@dataclass
class DimTemperature(BaseDimension):
    """
    Dimension class for the temperature data.
    """
    temperature_id: int = field(default=0)
    temperature: Decimal = field(default=Decimal('0.0'))
    feels_like: Decimal = field(default=Decimal('0.0'))
    temp_min: Optional[Decimal] = field(default=Decimal('inf'))
    temp_max: Optional[Decimal] = field(default=Decimal('inf'))
    temp_deviation: Optional[Decimal] = field(default=None)
    comfort_level: Optional[str] = field(default=None)
    temperature_category: Optional[str] = field(default=None)
    temp_range: Optional[Decimal] = field(default=Decimal('inf'))

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they were not provided."""
        # print("In DimTemperature: __post_init__")
        if self.comfort_level is None:
            self._comfort_level()
        if self.temperature_category is None:
            self._temperature_category()
        if self.temp_range == Decimal('inf'):
            self._temp_range()

    def _comfort_level(self) -> None:
        """Calculate comfort level based on temperature."""
        # return ComfortLevel.comfort_level(float(self.temperature))
        # print(f"In _comfort_level: {float(self.temperature)}")
        self.comfort_level = ComfortLevel.comfort_level(float(self.temperature), 'F')

    def _temperature_category(self) -> None:
        """Calculate temperature category."""
        # return TemperatureCategory.temperature_category(float(self.temperature))
        self.temperature_category = (
            TemperatureCategory.temperature_category(float(self.temperature), 'F'))

    def _temp_range(self) -> None:
        """Calculate temperature range if min and max are available."""
        # if self.temp_min is not None and self.temp_max is not None:
        #    return self.temp_max - self.temp_min
        # return None
        if (self.temp_min is not None and self.temp_max is not None and
                self.temp_min != Decimal('inf') and self.temp_max != Decimal('inf')):
            self.temp_range = self.temp_max - self.temp_min
        else:
            self.temp_range = Decimal('inf')

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimTemperature instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimTemperature attributes.

        Returns:
            DimTemperature: A new DimTemperature instance.
        """
        data_copy = data.copy()

        # Ensure proper type for temperature_id
        if 'temperature_id' in data_copy and not isinstance(data_copy['temperature_id'], int):
            data_copy['temperature_id'] = int(data_copy['temperature_id'])

        # Convert required Decimal fields
        for decimal_field in ['temperature', 'feels_like']:
            if decimal_field in data_copy and not isinstance(data_copy[decimal_field], Decimal):
                data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Convert optional Decimal fields
        for optional_decimal_field in ['temp_min', 'temp_max', 'temp_deviation']:
            if (optional_decimal_field in data_copy and
                    data_copy[optional_decimal_field] is not None and
                    not isinstance(data_copy[optional_decimal_field], Decimal)):
                data_copy[optional_decimal_field] = Decimal(str(data_copy[optional_decimal_field]))

        return cls(**data_copy)

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimTemperature":
        """
        Create a DimTemperature instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimTemperature attributes.

        Returns:
            DimTemperature: A new DimTemperature instance.
            :param data:
            :return:
        """
        data_copy = data.copy()

        # Ensure proper type for temperature_id
        if 'temperature_id' in data_copy and not isinstance(data_copy['temperature_id'], int):
            data_copy['temperature_id'] = int(data_copy['temperature_id'])

        # Convert required Decimal fields
        for decimal_field in ['temperature', 'feels_like']:
            if decimal_field in data_copy and not isinstance(data_copy[decimal_field], Decimal):
                data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Convert optional Decimal fields
        for optional_decimal_field in ['temp_min', 'temp_max', 'temp_deviation']:
            if (optional_decimal_field in data_copy and
                    data_copy[optional_decimal_field] is not None and
                    not isinstance(data_copy[optional_decimal_field], Decimal)):
                data_copy[optional_decimal_field] = Decimal(str(data_copy[optional_decimal_field]))

        # Must explicitly reference the DimTemperature class here
        return DimTemperature(**data_copy)


@dataclass
class DimPressure(BaseDimension):
    """
    Dimension class for the Pressure dataset.
    """
    pressure_id: int = field(default=0)
    pressure: int = field(default=0)
    sea_level_pressure: Optional[int] = field(default=None)
    ground_level_pressure: Optional[int] = field(default=None)
    pressure_trend: Optional[PressureTrend] = field(default=None)
    pressure_category: Optional[str] = field(default=None)
    is_normal_range: Optional[bool] = field(default=False)
    storm_potential: Optional[bool] = field(default=False)

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they were not provided."""
        # print("In DimPressure: __post_init__")
        if self.pressure_category is None:
            self._pressure_category()
        if self.is_normal_range is False:
            self._is_normal_range()
        if self.storm_potential is False:
            self._storm_potential()

    #   def _pressure_category(self) -> Optional[str]:
    def _pressure_category(self) -> None:
        """Calculate pressure category and set self.pressure_category.

        Returns:
            str: The pressure category description
        """
        thresholds = [980, 1000, 1015, 1025, 1040]
        categories = ["Very Low", "Low", "Normal Low", "Normal High", "High", "Very High"]

        index = bisect_right(thresholds, self.pressure)
        category = categories[index]

        self.pressure_category = category

    def _is_normal_range(self) -> None:
        """Check if pressure is in normal range."""
        self.is_normal_range = 1000 <= self.pressure <= 1025

    def _storm_potential(self) -> None:
        """Check if pressure indicates storm potential."""
        self.is_normal_range = self.pressure < 995

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimPressure instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimPressure attributes.

        Returns:
            DimPressure: A new DimPressure instance.
        """
        data_copy = data.copy()

        # Ensure proper type for pressure_id
        if 'pressure_id' in data_copy and not isinstance(data_copy['pressure_id'], int):
            data_copy['pressure_id'] = int(data_copy['pressure_id'])

        # Ensure proper type for pressure
        if 'pressure' in data_copy and not isinstance(data_copy['pressure'], int):
            data_copy['pressure'] = int(data_copy['pressure'])

        # Ensure proper types for optional int fields
        for int_field in ['sea_level_pressure', 'ground_level_pressure']:
            if (int_field in data_copy and data_copy[int_field]
                    is not None and not isinstance(data_copy[int_field], int)):
                data_copy[int_field] = int(data_copy[int_field])

        # Handle pressure_trend enum conversion
        if 'pressure_trend' in data_copy and data_copy['pressure_trend'] is not None:
            # If it's a string, convert to enum
            if isinstance(data_copy['pressure_trend'], str):
                try:
                    data_copy['pressure_trend'] = PressureTrend[data_copy['pressure_trend']]
                except KeyError:
                    # Try by value if not by name
                    try:
                        data_copy['pressure_trend'] = PressureTrend(data_copy['pressure_trend'])
                    except ValueError:
                        # Default to None if conversion fails
                        data_copy['pressure_trend'] = None
            # If it's an int, try to convert to enum by value
            elif isinstance(data_copy['pressure_trend'], int):
                try:
                    data_copy['pressure_trend'] = PressureTrend(data_copy['pressure_trend'])
                except ValueError:
                    # Default to None if conversion fails
                    data_copy['pressure_trend'] = None

        return cls(**data_copy)

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimPressure":
        """
        Create a DimPressure instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimPressure attributes.

        Returns:
            DimPressure: A new DimPressure instance.
        """
        data_copy = data.copy()

        # Ensure proper type for pressure_id
        if 'pressure_id' in data_copy and not isinstance(data_copy['pressure_id'], int):
            data_copy['pressure_id'] = int(data_copy['pressure_id'])

        # Ensure proper type for pressure
        if 'pressure' in data_copy and not isinstance(data_copy['pressure'], int):
            data_copy['pressure'] = int(data_copy['pressure'])

        # Ensure proper types for optional int fields
        for int_field in ['sea_level_pressure', 'ground_level_pressure']:
            if (int_field in data_copy and data_copy[int_field]
                    is not None and not isinstance(data_copy[int_field], int)):
                data_copy[int_field] = int(data_copy[int_field])

        # Handle pressure_trend enum conversion
        if 'pressure_trend' in data_copy and data_copy['pressure_trend'] is not None:
            # If it's a string, convert to enum
            if isinstance(data_copy['pressure_trend'], str):
                try:
                    data_copy['pressure_trend'] = PressureTrend[data_copy['pressure_trend']]
                except KeyError:
                    # Try by value if not by name
                    try:
                        data_copy['pressure_trend'] = PressureTrend(data_copy['pressure_trend'])
                    except ValueError:
                        # Default to None if conversion fails
                        data_copy['pressure_trend'] = None
            # If it's an int, try to convert to enum by value
            elif isinstance(data_copy['pressure_trend'], int):
                try:
                    data_copy['pressure_trend'] = PressureTrend(data_copy['pressure_trend'])
                except ValueError:
                    # Default to None if conversion fails
                    data_copy['pressure_trend'] = None

        # Must explicitly reference the DimPressure class here
        return DimPressure(**data_copy)


@dataclass
class DimHumidity(BaseDimension):
    """
    Dimension class for the Humidity dataset.
    """
    humidity_id: int = field(default=0)
    humidity: int = field(default=0)
    humidity_category: Optional[str] = field(default=None)
    comfort_impact: Optional[str] = field(default=None)
    is_dew_point_risk: Optional[bool] = field(default=False)
    mold_risk_level: Optional[str] = field(default=None)

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they were not provided.
        :rtype: object
        """
        # print("In DimHumidity: __post_init__")
        if self.humidity_category is None:
            self._humidity_category()
        if self.comfort_impact is None:
            self._comfort_impact()
        if self.is_dew_point_risk is False:
            self._is_dew_point_risk()
        if self.mold_risk_level is None:
            self._mold_risk_level()

    def _humidity_category(self) -> None:
        """Calculate humidity category.
        :rtype: object
        """
        # print(f"In _humidity_category: {self.humidity}")
        self.humidity_category = HumidityCategory.humidity_category(self.humidity)

    def _comfort_impact(self) -> None:
        """Describe comfort impact of humidity.
        :rtype: object
        """
        if self.humidity < 30:
            comfort_impact = "May cause dry skin and respiratory discomfort"
        elif self.humidity <= 50:
            comfort_impact = "Optimal comfort range"
        elif self.humidity <= 70:
            comfort_impact = "May feel sticky or muggy"
        else:
            comfort_impact = "Uncomfortable, potential heat stress when warm"

        self.comfort_impact = comfort_impact

    def _is_dew_point_risk(self) -> None:
        """Check if humidity indicates dew point risk.
        :rtype: object
        """
        self.is_dew_point_risk = self.humidity > 85

    def _mold_risk_level(self) -> None:
        """Calculate mold risk based on humidity.
        :rtype: object
        """
        if self.humidity < 60:
            mold_risk_level = "Low"
        elif self.humidity <= 80:
            mold_risk_level = "Moderate"
        else:
            mold_risk_level = "High"

        self.mold_risk_level = mold_risk_level

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimHumidity instance from a dictionary.

        Args:
            data: Dictionary containing the humidity data

        Returns:
            A new DimHumidity instance
        """
        # Get values from dict with fallback to defaults if keys don't exist
        humidity_id = data.get('humidity_id', 0)
        humidity = data.get('humidity', 0)

        # Create a new instance using the base class's constructor
        # Assuming BaseDimension might have its own required parameters
        base_params = {k: v for k, v in data.items()
                       if k not in ('humidity_id', 'humidity')}

        # Create and return instance with all parameters
        return cls(
            humidity_id=humidity_id,
            humidity=humidity,
            **base_params
        )

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimHumidity":
        """
        Create a DimHumidity instance from a dictionary.

        Args:
            data: Dictionary containing the humidity data

        Returns:
            A new DimHumidity instance
        """
        # Get values from dict with fallback to defaults if keys don't exist
        humidity_id = data.get('humidity_id', 0)
        humidity = data.get('humidity', 0)

        # Filter out keys specific to this class
        base_params = {k: v for k, v in data.items()
                       if k not in ('humidity_id', 'humidity')}

        # Create and return instance with all parameters
        return DimHumidity(
            humidity_id=humidity_id,
            humidity=humidity,
            **base_params
        )


@dataclass
class DimPrecipitation(BaseDimension):
    """
    Dimension class for the Precipitation dataset.
    """
    precipitation_id: int = field(default=0)
    # Assuming NONE is a valid enum value
    precipitation_type: PrecipitationType = field(default=PrecipitationType.NONE)
    volume_1h: Optional[Decimal] = field(default=None)
    volume_3h: Optional[Decimal] = field(default=None)
    intensity_category: Optional[str] = field(default=None)
    impact_level: Optional[str] = field(default=None)
    flooding_risk: Optional[str] = field(default=None)

    def __post_init__(self) -> None:
        """Calculate and set derived fields if they were not provided.
        :rtype: object
        """
        # print("In DimPrecipitation: __post_init__")
        if self.intensity_category is None:
            self._intensity_category()
        if self.impact_level is None:
            self._impact_level()
        if self.flooding_risk is None:
            self._flooding_risk()

    def _intensity_category(self) -> None:
        """Calculate precipitation intensity category.
        :rtype: object
        """
        intensity_category = IntensityCategory.NONE
        if self.precipitation_type == PrecipitationType.NONE:
            intensity_category = IntensityCategory.NONE

        # Use 1h volume if available
        if self.volume_1h is not None:
            # vol = float(self.volume_1h)
            intensity_category = IntensityCategory.intensity_category(float(self.volume_1h))
        # Use 3h volume if 1h not available
        elif self.volume_3h is not None:
            # vol = float(self.volume_3h)
            intensity_category = IntensityCategory.intensity_category(float(self.volume_3h))

        self.intensity_category = intensity_category

    def _impact_level(self) -> None:
        """Calculate precipitation impact level.
        :rtype: object
        """
        # Check for no precipitation
        no_precip_type = self.precipitation_type == PrecipitationType.NONE
        zero_1h_volume = self.volume_1h is not None and float(self.volume_1h) == 0
        zero_3h_volume = (self.volume_1h is None and
                          self.volume_3h is not None and
                          float(self.volume_3h) == 0)

        if no_precip_type or zero_1h_volume or zero_3h_volume:
            self.impact_level = "none"
            return

        # Check for severe precipitation
        is_rain_or_snow = (
                self.precipitation_type in [PrecipitationType.RAIN, PrecipitationType.SNOW])
        heavy_1h = self.volume_1h is not None and float(self.volume_1h) > 15
        heavy_3h = self.volume_3h is not None and float(self.volume_3h) > 45

        if is_rain_or_snow and (heavy_1h or heavy_3h):
            self.impact_level = "severe"
            return

        # Check for significant precipitation
        includes_sleet = self.precipitation_type in [PrecipitationType.RAIN, PrecipitationType.SNOW,
                                                     PrecipitationType.SLEET]
        significant_1h = self.volume_1h is not None and 7.5 <= float(self.volume_1h) <= 15
        significant_3h = self.volume_3h is not None and 22.5 <= float(self.volume_3h) <= 45

        if includes_sleet and (significant_1h or significant_3h):
            self.impact_level = "significant"
            return

        # Check for moderate precipitation
        moderate_1h = self.volume_1h is not None and 2.5 <= float(self.volume_1h) < 7.5
        moderate_3h = self.volume_3h is not None and 7.5 <= float(self.volume_3h) < 22.5

        if moderate_1h or moderate_3h:
            self.impact_level = "moderate"
            return

        # Default to minimal impact
        self.impact_level = "minimal"

    def _flooding_risk(self) -> None:
        """Calculate flooding risk.
        :rtype: object
        """
        # Default to "none" for non-rain precipitation
        if self.precipitation_type != PrecipitationType.RAIN:
            self.flooding_risk = "none"
            return

        # For rain, determine risk based on volume
        high_1h = self.volume_1h is not None and float(self.volume_1h) > 10
        high_3h = self.volume_3h is not None and float(self.volume_3h) > 30

        if high_1h or high_3h:
            self.flooding_risk = "high"
            return

        moderate_1h = self.volume_1h is not None and 5 <= float(self.volume_1h) <= 10
        moderate_3h = self.volume_3h is not None and 15 <= float(self.volume_3h) <= 30

        if moderate_1h or moderate_3h:
            self.flooding_risk = "moderate"
            return

        # If it's rain but below moderate thresholds
        self.flooding_risk = "low"

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimPrecipitation instance from a dictionary.

        Args:
            data: Dictionary containing precipitation data

        Returns:
            A new DimPrecipitation instance
        """
        # Extract precipitation-specific fields with defaults
        precipitation_id = data.get('precipitation_id', 0)

        # Handle enum conversion - assuming string representation in dict
        precipitation_type_raw = data.get('precipitation_type', PrecipitationType.NONE)
        if isinstance(precipitation_type_raw, str):
            try:
                precipitation_type = PrecipitationType[precipitation_type_raw]
            except KeyError:
                precipitation_type = PrecipitationType.NONE
        else:
            precipitation_type = precipitation_type_raw

        # Handle Decimal conversions for volume fields
        volume_1h_raw = data.get('volume_1h')
        volume_1h = Decimal(volume_1h_raw) if volume_1h_raw is not None else None

        volume_3h_raw = data.get('volume_3h')
        volume_3h = Decimal(volume_3h_raw) if volume_3h_raw is not None else None

        # Get remaining fields for the base class
        base_params = {k: v for k, v in data.items()
                       if k not in ('precipitation_id', 'precipitation_type',
                                    'volume_1h', 'volume_3h')}

        # Create and return the instance using cls reference
        return cls(
            precipitation_id=precipitation_id,
            precipitation_type=precipitation_type,
            volume_1h=volume_1h,
            volume_3h=volume_3h,
            **base_params
        )

    # Static method to explicitly references DimWeatherCondition in the return statement
    # rather than using a class reference
    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimPrecipitation":
        """
        Create a DimPrecipitation instance from a dictionary.

        Args:
            data: Dictionary containing precipitation data

        Returns:
            A new DimPrecipitation instance
        """
        # Extract precipitation-specific fields with defaults
        precipitation_id = data.get('precipitation_id', 0)

        # Handle enum conversion - assuming string representation in dict
        precipitation_type_raw = data.get('precipitation_type', PrecipitationType.NONE)
        if isinstance(precipitation_type_raw, str):
            try:
                precipitation_type = PrecipitationType[precipitation_type_raw]
            except KeyError:
                precipitation_type = PrecipitationType.NONE
        else:
            precipitation_type = precipitation_type_raw

        # Handle Decimal conversions for volume fields
        volume_1h_raw = data.get('volume_1h')
        volume_1h = Decimal(volume_1h_raw) if volume_1h_raw is not None else None

        volume_3h_raw = data.get('volume_3h')
        volume_3h = Decimal(volume_3h_raw) if volume_3h_raw is not None else None

        # Get remaining fields for the base class
        base_params = {k: v for k, v in data.items()
                       if k not in ('precipitation_id', 'precipitation_type',
                                    'volume_1h', 'volume_3h')}

        # Create and return the instance
        return DimPrecipitation(
            precipitation_id=precipitation_id,
            precipitation_type=precipitation_type,
            volume_1h=volume_1h,
            volume_3h=volume_3h,
            **base_params
        )
