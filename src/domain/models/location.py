"""
This module contains the domain models for the City and location-related entities
following DDD principles.
The domain models represent core business concepts and encapsulate both data and behavior.
"""
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Optional, Dict, Any

from .common import Entity, GeoCoordinate


# pylint: disable=too-many-instance-attributes
@dataclass
class City(Entity):
    """Domain entity representing a city."""
    name: str = field(default_factory=str)
    country: str = field(default_factory=str)
    coordinates: Optional[GeoCoordinate] = field(default=None)
    # Store names by language code
    local_names: Dict[str, str] = field(default_factory=dict)
    state: Optional[str] = field(default_factory=str)
    timezone: Optional[int] = field(default_factory=int)
    population: Optional[int] = field(default_factory=int)
    sunrise: Optional[datetime] = field(default_factory=datetime.now)
    sunset: Optional[datetime] = field(default_factory=datetime.now)
    elevation_meters: Optional[int] = field(default_factory=int)
    region: Optional[str] = field(default_factory=str)

    def get_name(self, lang_code: str = '') -> str:
        """Get the city name in the specified language if available."""
        if lang_code and lang_code in self.local_names:
            return self.local_names[lang_code]
        return self.name

    @property
    def utc_offset(self) -> timedelta:
        """Calculate the timezone offset as a timedelta."""
        if self.timezone is None:
            return timedelta(hours=0)
        return timedelta(seconds=self.timezone)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'City':
        """Create a City instance from a dictionary."""
        data_copy = data.copy()

        # Handle latitude and longitude
        lat = data_copy.pop('latitude', None)
        lng = data_copy.pop('longitude', None)
        coordinates = None
        if lat is not None and lng is not None:
            if not isinstance(lat, Decimal):
                lat = Decimal(str(lat))
            if not isinstance(lng, Decimal):
                lng = Decimal(str(lng))
            coordinates = GeoCoordinate(latitude=lat, longitude=lng)

        # Handle local_names
        local_names = data_copy.pop('local_names', {})
        if not isinstance(local_names, dict):
            local_names = {}

        # Handle datetime conversions
        for dt_field in ['sunrise', 'sunset', 'created_at', 'updated_at']:
            if dt_field in data_copy and isinstance(data_copy[dt_field], str):
                data_copy[dt_field] = datetime.fromisoformat(data_copy[dt_field])

        # Create instance with processed data
        return cls.create(
            coordinates=coordinates,
            local_names=local_names,
            **data_copy
        )
