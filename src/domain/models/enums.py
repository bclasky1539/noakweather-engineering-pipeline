"""
This module contains the domain enums for the weather system following DDD principles.
"""
from bisect import bisect_right
from enum import Enum
from typing import List


# ------------------
# Domain Enums
# ------------------

class PrecipitationType(str, Enum):
    """Precipitation types for weather system."""
    RAIN = "rain"
    SNOW = "snow"
    SLEET = "sleet"
    HAIL = "hail"
    NONE = "none"


class WindDirection(str, Enum):
    """Cardinal wind directions."""
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

    @classmethod
    def from_degrees(cls, degrees: int) -> 'WindDirection':
        """Convert degrees to cardinal direction."""
        # Convert degrees to an index from 0-15 (16 directions)
        index = int((degrees + 11.25) % 360 / 22.5)

        # Map the index to the appropriate WindDirection enum
        directions = [
            cls.N, cls.NNE, cls.NE, cls.ENE,
            cls.E, cls.ESE, cls.SE, cls.SSE,
            cls.S, cls.SSW, cls.SW, cls.WSW,
            cls.W, cls.WNW, cls.NW, cls.NNW
        ]
        return directions[index]


class BeaufortCategory(Enum):
    """Beaufort wind force categories."""
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

    @classmethod
    def boundaries(cls) -> List[float]:
        """Return the upper boundaries of each Beaufort category."""
        return [0.5, 1.5, 3.3, 5.5, 7.9, 10.7, 13.8, 17.1, 20.7, 24.4, 28.4, 32.6, float('inf')]

    @classmethod
    def categories(cls) -> List['BeaufortCategory']:
        """Return all categories in order."""
        return [
            cls.CALM, cls.LIGHT_AIR, cls.LIGHT_BREEZE, cls.GENTLE_BREEZE,
            cls.MODERATE_BREEZE, cls.FRESH_BREEZE, cls.STRONG_BREEZE, cls.NEAR_GALE,
            cls.GALE, cls.STRONG_GALE, cls.STORM, cls.VIOLENT_STORM, cls.HURRICANE
        ]

    @classmethod
    def from_speed(cls, speed: float) -> 'BeaufortCategory':
        """Get the Beaufort category for the given wind speed."""
        index = bisect_right(cls.boundaries(), speed)
        return cls.categories()[index]

    @classmethod
    def scale_number(cls, speed: float) -> int:
        """Get the Beaufort scale number (0-12) for the given wind speed."""
        index = bisect_right(cls.boundaries(), speed)
        return index


class ComfortLevel(str, Enum):
    """Temperature comfort levels."""
    VERY_COLD = "Very Cold"
    COLD = "Cold"
    COOL = "Cool"
    COMFORTABLE = "Comfortable"
    WARM = "Warm"
    HOT = "Hot"
    VERY_HOT = "Very Hot"

    @classmethod
    def boundaries_celsius(cls) -> List[float]:
        """Return the boundaries in Celsius."""
        return [-10.0, 0.0, 10.0, 18.0, 24.0, 29.0, 35.0]

    @classmethod
    def categories(cls) -> List['ComfortLevel']:
        """Return all categories in order."""
        return [
            cls.VERY_COLD, cls.COLD, cls.COOL, cls.COMFORTABLE,
            cls.WARM, cls.HOT, cls.VERY_HOT
        ]

    @classmethod
    def from_temperature(cls, temperature: float, unit: str = 'C') -> 'ComfortLevel':
        """Get the Comfort Level for the given temperature."""
        # Convert to Celsius if needed
        temp_celsius = temperature
        if unit.upper() == 'F':
            temp_celsius = (temperature - 32) * 5 / 9
        elif unit.upper() == 'K':
            temp_celsius = temperature - 273.15

        index = bisect_right(cls.boundaries_celsius(), temp_celsius)
        return cls.categories()[index]


class TemperatureCategory(str, Enum):
    """More detailed temperature categories."""
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
    def boundaries_celsius(cls) -> List[float]:
        """Return the boundaries in Celsius."""
        return [-20.0, -10.0, 0.0, 10.0, 18.0, 24.0, 29.0, 35.0]

    @classmethod
    def categories(cls) -> List['TemperatureCategory']:
        """Return all categories in order."""
        return [
            cls.EXTREME_COLD, cls.VERY_COLD, cls.FREEZING, cls.COLD, cls.COOL,
            cls.MILD, cls.WARM, cls.HOT, cls.VERY_HOT
        ]

    @classmethod
    def from_temperature(cls, temperature: float, unit: str = 'C') -> 'TemperatureCategory':
        """Calculate temperature category."""
        # Convert to Celsius if needed
        temp_celsius = temperature
        if unit.upper() == 'F':
            temp_celsius = (temperature - 32) * 5 / 9
        elif unit.upper() == 'K':
            temp_celsius = temperature - 273.15

        index = bisect_right(cls.boundaries_celsius(), temp_celsius)
        return cls.categories()[index]


class PressureTrend(str, Enum):
    """Barometric pressure trends."""
    FALLING = "falling"
    STABLE = "stable"
    RISING = "rising"
    UNKNOWN = "unknown"


class HumidityCategory(str, Enum):
    """Humidity level categories."""
    DRY = "dry"
    COMFORTABLE = "comfortable"
    HUMID = "humid"
    VERY_HUMID = "very_humid"

    @classmethod
    def boundaries(cls) -> List[int]:
        """Return the boundaries for humidity categories."""
        return [30, 50, 70, 100]

    @classmethod
    def categories(cls) -> List['HumidityCategory']:
        """Return all categories in order."""
        return [cls.DRY, cls.COMFORTABLE, cls.HUMID, cls.VERY_HUMID]

    @classmethod
    def from_humidity(cls, humidity: int) -> 'HumidityCategory':
        """Calculate humidity category from percentage."""
        index = bisect_right(cls.boundaries(), humidity)
        return cls.categories()[index]


class IntensityCategory(str, Enum):
    """Precipitation intensity categories."""
    NONE = "none"
    VERY_LIGHT = "very_light"
    LIGHT = "light"
    MODERATE = "moderate"
    HEAVY = "heavy"
    EXTREME = "extreme"

    @classmethod
    def boundaries(cls) -> List[float]:
        """Return the boundaries for intensity categories."""
        return [0.0, 10.0, 30.0, 50.0, 70.0, float('inf')]

    @classmethod
    def categories(cls) -> List['IntensityCategory']:
        """Return all categories in order."""
        return [
            cls.NONE, cls.VERY_LIGHT, cls.LIGHT, cls.MODERATE,
            cls.HEAVY, cls.EXTREME
        ]

    @classmethod
    def from_volume(cls, volume: float) -> 'IntensityCategory':
        """Calculate intensity category from precipitation volume."""
        index = bisect_right(cls.boundaries(), volume)
        return cls.categories()[index]
