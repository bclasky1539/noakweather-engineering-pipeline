"""
This module contains the domain models for the weather system following DDD principles.
The domain models represent core business concepts and encapsulate both data and behavior.
"""
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import Optional, Any


# ------------------
# Value Objects
# ------------------

class GeoCoordinate:
    """Value object representing geographical coordinates."""

    def __init__(self, latitude: Decimal, longitude: Decimal):
        self._validate_coordinates(latitude, longitude)
        self.latitude = latitude
        self.longitude = longitude

    def _validate_coordinates(self, latitude: Decimal, longitude: Decimal) -> None:
        """Validate the latitude and longitude values."""
        if not -90 <= float(latitude) <= 90:
            raise ValueError(f"Latitude must be between -90 and 90, got {latitude}")
        if not -180 <= float(longitude) <= 180:
            raise ValueError(f"Longitude must be between -180 and 180, got {longitude}")

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, GeoCoordinate):
            return False
        return self.latitude == other.latitude and self.longitude == other.longitude

    def __hash__(self) -> int:
        return hash((self.latitude, self.longitude))

    def __repr__(self) -> str:
        return f"GeoCoordinate(latitude={self.latitude}, longitude={self.longitude})"

    @property
    def as_tuple(self) -> tuple[float, float]:
        """Return coordinates as a (longitude, latitude) tuple."""
        return (float(self.longitude), float(self.latitude))


# ------------------
# Domain Models
# ------------------

@dataclass
class Entity:
    """Base class for all domain entities."""
    id: str = field(default_factory=str)
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: Optional[datetime] = field(default_factory=datetime.now)

    @classmethod
    def create(cls, **kwargs: Any) -> Any:
        """Factory method to create a new entity with generated ID."""
        entity_id = kwargs.pop('id', str(uuid.uuid4()))
        return cls(id=entity_id, **kwargs)
