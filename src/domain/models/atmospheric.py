"""
This module contains the domain models for the Pressure, Humidity entities
following DDD principles.
The domain models represent core business concepts and encapsulate both data and behavior.
"""
from bisect import bisect_right
from dataclasses import dataclass, field
from typing import Optional, Dict, List, Any, ClassVar

from .common import Entity
from .enums import PressureTrend, HumidityCategory


@dataclass
class Pressure(Entity):
    """Domain entity representing atmospheric pressure."""
    pressure: int = field(default_factory=int)
    sea_level_pressure: Optional[int] = field(default_factory=int)
    ground_level_pressure: Optional[int] = field(default_factory=int)
    pressure_trend: Optional[PressureTrend] = field(default=None)
    pressure_category: Optional[str] = field(default_factory=str)
    is_normal_range: Optional[bool] = field(default_factory=bool)
    storm_potential: Optional[bool] = field(default_factory=bool)

    PRESSURE_THRESHOLDS: ClassVar[List[int]] = [980, 1000, 1015, 1025, 1040]
    PRESSURE_CATEGORIES: ClassVar[List[str]] = [
        "Very Low", "Low", "Normal Low", "Normal High", "High", "Very High"
    ]

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if self.pressure_category is None:
            self._calculate_pressure_category()
        if self.is_normal_range is None:
            self._calculate_normal_range()
        if self.storm_potential is None:
            self._calculate_storm_potential()

    def _calculate_pressure_category(self) -> None:
        """Calculate pressure category based on thresholds."""
        index = bisect_right(self.PRESSURE_THRESHOLDS, self.pressure)
        self.pressure_category = self.PRESSURE_CATEGORIES[index]

    def _calculate_normal_range(self) -> None:
        """Check if pressure is in normal range (1000-1025 hPa)."""
        self.is_normal_range = 1000 <= self.pressure <= 1025

    def _calculate_storm_potential(self) -> None:
        """Check if pressure indicates storm potential (< 995 hPa)."""
        self.storm_potential = self.pressure < 995

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Pressure':
        """Create a Pressure instance from a dictionary."""
        data_copy = data.copy()

        # Ensure int fields are properly typed
        for int_field in ['pressure', 'sea_level_pressure', 'ground_level_pressure']:
            if int_field in data_copy and data_copy[int_field] is not None:
                data_copy[int_field] = int(data_copy[int_field])

        # Handle pressure_trend enum
        if 'pressure_trend' in data_copy and data_copy['pressure_trend'] is not None:
            if isinstance(data_copy['pressure_trend'], str):
                try:
                    data_copy['pressure_trend'] = PressureTrend(data_copy['pressure_trend'])
                except ValueError:
                    data_copy['pressure_trend'] = None

        return cls.create(**data_copy)


@dataclass
class Humidity(Entity):
    """Domain entity representing humidity measurements."""
    humidity: int = field(default_factory=int)
    humidity_category: Optional[HumidityCategory] = field(default=None)
    comfort_impact: Optional[str] = field(default_factory=str)
    is_dew_point_risk: Optional[bool] = field(default_factory=bool)
    mold_risk_level: Optional[str] = field(default_factory=str)

    def __post_init__(self) -> None:
        """Calculate derived fields if necessary."""
        if self.humidity_category is None:
            self._calculate_humidity_category()
        if self.comfort_impact is None:
            self._calculate_comfort_impact()
        if self.is_dew_point_risk is None:
            self._calculate_dew_point_risk()
        if self.mold_risk_level is None:
            self._calculate_mold_risk()

    def _calculate_humidity_category(self) -> None:
        """Calculate humidity category."""
        self.humidity_category = HumidityCategory.from_humidity(self.humidity)

    def _calculate_comfort_impact(self) -> None:
        """Describe comfort impact of humidity."""
        if self.humidity < 30:
            self.comfort_impact = "May cause dry skin and respiratory discomfort"
        elif self.humidity <= 50:
            self.comfort_impact = "Optimal comfort range"
        elif self.humidity <= 70:
            self.comfort_impact = "May feel sticky or muggy"
        else:
            self.comfort_impact = "Uncomfortable, potential heat stress when warm"

    def _calculate_dew_point_risk(self) -> None:
        """Check if humidity indicates dew point risk."""
        self.is_dew_point_risk = self.humidity > 85

    def _calculate_mold_risk(self) -> None:
        """Calculate mold risk based on humidity."""
        if self.humidity < 60:
            self.mold_risk_level = "Low"
        elif self.humidity <= 80:
            self.mold_risk_level = "Moderate"
        else:
            self.mold_risk_level = "High"

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Humidity':
        """Create a Humidity instance from a dictionary."""
        data_copy = data.copy()

        # Ensure humidity is an int
        if 'humidity' in data_copy and not isinstance(data_copy['humidity'], int):
            data_copy['humidity'] = int(data_copy['humidity'])

        # Handle humidity_category enum
        if 'humidity_category' in data_copy and isinstance(data_copy['humidity_category'], str):
            try:
                data_copy['humidity_category'] = HumidityCategory(data_copy['humidity_category'])
            except ValueError:
                data_copy['humidity_category'] = None

        # Convert boolean fields
        if 'is_dew_point_risk' in data_copy and isinstance(data_copy['is_dew_point_risk'], str):
            data_copy['is_dew_point_risk'] = data_copy['is_dew_point_risk'].lower() \
                                             in ['true', 't', 'yes', 'y', '1']

        return cls.create(**data_copy)
