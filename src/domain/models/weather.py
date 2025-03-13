"""
This module contains the domain models for the WeatherCondition, Temperature,
Wind, Precipitation entities following DDD principles.
The domain models represent core business concepts and encapsulate both data and behavior.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Optional, List, Dict, Any, ClassVar

from .common import Entity
from .enums import (BeaufortCategory, WindDirection, ComfortLevel, TemperatureCategory,
                    PrecipitationType, IntensityCategory)


@dataclass
class WeatherCondition(Entity):
    """Domain entity representing weather conditions."""
    condition_main: str = field(default_factory=str)
    description: str = field(default_factory=str)
    condition_id: int = field(default_factory=int)
    icon_code: str = field(default_factory=str)
    severity_level: Optional[int] = field(default_factory=int)
    is_precipitation: bool = field(default_factory=bool)
    is_extreme: bool = field(default_factory=bool)

    EXTREME_RANGES: ClassVar[List[tuple]] = [
        (202, 232),  # Severe thunderstorms
        (504, 531),  # Extreme rain events
        (602, 622),  # Heavy snow events
        (751, 781),  # Severe atmospheric conditions
        (900, 906),  # Extreme additional conditions
        (961, 962)  # Violent storms
    ]

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if not self.is_extreme:
            self._determine_extreme_status()

    def _determine_extreme_status(self) -> None:
        """Determine if a weather condition is considered extreme."""
        self.is_extreme = any(
            start <= self.condition_id <= end
            for start, end in self.EXTREME_RANGES
        )

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'WeatherCondition':
        """Create a WeatherCondition instance from a dictionary."""
        data_copy = data.copy()

        # Ensure integers are properly typed
        for int_field in ['condition_id', 'severity_level']:
            if int_field in data_copy and data_copy[int_field] is not None:
                data_copy[int_field] = int(data_copy[int_field])

        # Convert boolean fields from strings if needed
        for bool_field in ['is_precipitation', 'is_extreme']:
            if bool_field in data_copy and isinstance(data_copy[bool_field], str):
                data_copy[bool_field] = (data_copy[bool_field].lower()
                                         in ['true', 't', 'yes', 'y', '1'])

        return cls.create(**data_copy)


@dataclass
class Wind(Entity):
    """Domain entity representing wind conditions."""
    speed: Decimal = field(default_factory=Decimal)
    degrees: int = field(default_factory=int)
    direction_cardinal: Optional[WindDirection] = field(default=None)
    gust_speed: Optional[Decimal] = field(default_factory=Decimal)
    beaufort_scale: Optional[int] = field(default_factory=int)
    beaufort_description: Optional[str] = field(default_factory=str)

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if self.direction_cardinal is None:
            self._calculate_direction()
        if self.beaufort_scale is None:
            self._calculate_beaufort()

    def _calculate_direction(self) -> None:
        """Calculate the cardinal direction based on degrees."""
        self.direction_cardinal = WindDirection.from_degrees(self.degrees)

    def _calculate_beaufort(self) -> None:
        """Calculate the Beaufort scale values."""
        category = BeaufortCategory.from_speed(float(self.speed))
        self.beaufort_scale = BeaufortCategory.scale_number(float(self.speed))
        self.beaufort_description = category.value

    @property
    def is_gusty(self) -> bool:
        """Determine if wind is gusty (gust > speed * 1.3)."""
        if self.gust_speed is None:
            return False
        return float(self.gust_speed) > float(self.speed) * 1.3

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Wind':
        """Create a Wind instance from a dictionary."""
        data_copy = data.copy()

        # Ensure proper type for speed and gust_speed
        for decimal_field in ['speed', 'gust_speed']:
            if decimal_field in data_copy and data_copy[decimal_field] is not None:
                if not isinstance(data_copy[decimal_field], Decimal):
                    data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Ensure proper type for degrees
        if 'degrees' in data_copy and not isinstance(data_copy['degrees'], int):
            data_copy['degrees'] = int(data_copy['degrees'])

        # Handle direction_cardinal if it's a string
        if 'direction_cardinal' in data_copy and isinstance(data_copy['direction_cardinal'], str):
            try:
                data_copy['direction_cardinal'] = WindDirection(data_copy['direction_cardinal'])
            except ValueError:
                data_copy['direction_cardinal'] = None

        return cls.create(**data_copy)


# pylint: disable=too-many-instance-attributes
@dataclass
class Temperature(Entity):
    """Domain entity representing temperature measurements."""
    temperature: Decimal = field(default_factory=Decimal)
    feels_like: Decimal = field(default_factory=Decimal)
    temp_min: Optional[Decimal] = field(default_factory=Decimal)
    temp_max: Optional[Decimal] = field(default_factory=Decimal)
    comfort_level: Optional[ComfortLevel] = field(default=None)
    temperature_category: Optional[TemperatureCategory] = field(default= None)
    temp_deviation: Optional[Decimal] = field(default_factory=Decimal)
    temp_range: Optional[Decimal] = field(default_factory=Decimal)
    unit: str = field(default='C')  # C for Celsius, F for Fahrenheit, K for Kelvin

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if self.comfort_level is None:
            self._calculate_comfort_level()
        if self.temperature_category is None:
            self._calculate_temperature_category()
        if self.temp_range is None and self.temp_min is not None and self.temp_max is not None:
            self._calculate_temp_range()

    def _calculate_comfort_level(self) -> None:
        """Calculate comfort level based on temperature."""
        self.comfort_level = ComfortLevel.from_temperature(float(self.temperature), self.unit)

    def _calculate_temperature_category(self) -> None:
        """Calculate temperature category."""
        self.temperature_category = TemperatureCategory.from_temperature(
            float(self.temperature), self.unit
        )

    def _calculate_temp_range(self) -> None:
        """Calculate temperature range if min and max are available."""
        if self.temp_min is not None and self.temp_max is not None:
            self.temp_range = self.temp_max - self.temp_min

    def to_celsius(self) -> Decimal:
        """Convert temperature to Celsius."""
        temp = self.temperature
        if self.unit == 'F':
            temp = (temp - 32) * Decimal('5') / Decimal('9')
        elif self.unit == 'K':
            temp = temp - Decimal('273.15')
        return temp

    def to_fahrenheit(self) -> Decimal:
        """Convert temperature to Fahrenheit."""
        celsius = self.to_celsius()
        return celsius * Decimal('9') / Decimal('5') + 32

    def to_kelvin(self) -> Decimal:
        """Convert temperature to Kelvin."""
        celsius = self.to_celsius()
        return celsius + Decimal('273.15')

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Temperature':
        """Create a Temperature instance from a dictionary."""
        data_copy = data.copy()

        # Process unit
        unit = data_copy.pop('unit', 'C')

        # Convert decimal fields
        for decimal_field in ['temperature', 'feels_like', 'temp_min',
                              'temp_max', 'temp_deviation']:
            if decimal_field in data_copy and data_copy[decimal_field] is not None:
                if not isinstance(data_copy[decimal_field], Decimal):
                    data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Convert enum fields
        if 'comfort_level' in data_copy and isinstance(data_copy['comfort_level'], str):
            try:
                data_copy['comfort_level'] = ComfortLevel(data_copy['comfort_level'])
            except ValueError:
                data_copy['comfort_level'] = None

        if 'temperature_category' in data_copy and \
                isinstance(data_copy['temperature_category'], str):
            try:
                data_copy['temperature_category'] =(
                    TemperatureCategory(data_copy['temperature_category']))
            except ValueError:
                data_copy['temperature_category'] = None

        return cls.create(unit=unit, **data_copy)


@dataclass
class Precipitation(Entity):
    """Domain entity representing precipitation measurements."""
    precipitation_type: PrecipitationType = field(default=PrecipitationType.NONE)
    volume_1h: Optional[Decimal] = field(default_factory=Decimal)
    volume_3h: Optional[Decimal] = field(default_factory=Decimal)
    intensity_category: Optional[IntensityCategory] = field(default=None)
    impact_level: Optional[str] = field(default_factory=str)
    flooding_risk: Optional[str] = field(default_factory=str)

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if self.intensity_category is None:
            self._calculate_intensity()
        if self.impact_level is None:
            self._calculate_impact()
        if self.flooding_risk is None:
            self._calculate_flooding_risk()

    def _calculate_intensity(self) -> None:
        """Calculate precipitation intensity category."""
        if self.precipitation_type == PrecipitationType.NONE:
            self.intensity_category = IntensityCategory.NONE
            return

        # Use 1h volume if available, otherwise use 3h
        volume = None
        if self.volume_1h is not None:
            volume = float(self.volume_1h)
        elif self.volume_3h is not None:
            volume = float(self.volume_3h)

        if volume is not None:
            self.intensity_category = IntensityCategory.from_volume(volume)
        else:
            self.intensity_category = IntensityCategory.NONE

    def _calculate_impact(self) -> None:
        """Calculate precipitation impact level."""
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
        is_rain_or_snow = self.precipitation_type in \
                          [PrecipitationType.RAIN, PrecipitationType.SNOW]
        heavy_1h = self.volume_1h is not None and float(self.volume_1h) > 15
        heavy_3h = self.volume_3h is not None and float(self.volume_3h) > 45

        if is_rain_or_snow and (heavy_1h or heavy_3h):
            self.impact_level = "severe"
            return

        # Check for significant precipitation
        includes_sleet = self.precipitation_type in [
            PrecipitationType.RAIN, PrecipitationType.SNOW, PrecipitationType.SLEET
        ]
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

    def _calculate_flooding_risk(self) -> None:
        """Calculate flooding risk based on precipitation type and volume."""
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

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Precipitation':
        """Create a Precipitation instance from a dictionary."""
        data_copy = data.copy()

        # Handle precipitation_type enum
        if 'precipitation_type' in data_copy:
            if isinstance(data_copy['precipitation_type'], str):
                try:
                    data_copy['precipitation_type'] =(
                        PrecipitationType(data_copy['precipitation_type']))
                except ValueError:
                    data_copy['precipitation_type'] = PrecipitationType.NONE

        # Convert decimal fields
        for decimal_field in ['volume_1h', 'volume_3h']:
            if decimal_field in data_copy and data_copy[decimal_field] is not None:
                if not isinstance(data_copy[decimal_field], Decimal):
                    data_copy[decimal_field] = Decimal(str(data_copy[decimal_field]))

        # Handle intensity_category enum
        if 'intensity_category' in data_copy and isinstance(data_copy['intensity_category'], str):
            try:
                data_copy['intensity_category'] = IntensityCategory(data_copy['intensity_category'])
            except ValueError:
                data_copy['intensity_category'] = None

        return cls.create(**data_copy)
