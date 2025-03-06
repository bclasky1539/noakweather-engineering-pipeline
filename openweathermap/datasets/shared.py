"""
This python script is for the shared dataclasses used for the current weather
and forecast for the openweather API.
"""
from dataclasses import dataclass, field
from datetime import date, time, datetime, timedelta
from decimal import Decimal
from typing import Optional, Dict, Any, List, Self

from openweathermap.datasets.weather import (PrecipitationType, DimWind, DimTemperature,
                                             PressureTrend, DimPressure, DimHumidity,
                                             DimPrecipitation)


# Dimension classes
@dataclass
class DimDate:
    """
    DimDate dataclass.
    """
    date_id: date = field()
    year: int = field()
    quarter: int = field()
    month: int = field()
    day: int = field()
    day_of_week: int = field()
    week_of_year: int = field()
    is_weekend: bool = field()
    season: str = field()
    month_name: str = field()
    day_name: str = field()
    is_holiday: bool = field(default=False)

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimDate instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimDate attributes.
                         The date_id key should contain a date string in ISO format (YYYY-MM-DD).

        Returns:
            DimDate: A new DimDate instance.
        """
        # Handle date_id conversion if it's a string
        if isinstance(data.get('date_id'), str):
            data = data.copy()  # Create a copy to avoid modifying the input
            data['date_id'] = date.fromisoformat(data['date_id'])

        # Handle boolean conversion for is_weekend and is_holiday if they're strings
        for bool_field in ['is_weekend', 'is_holiday']:
            if bool_field in data and isinstance(data[bool_field], str):
                data[bool_field] = data[bool_field].lower() in ['true', 't', 'yes', 'y', '1']

        return cls(**data)

    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimDate":
        """
        Create a DimDate instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimDate attributes.
                         The date_id key should contain a date string in ISO format (YYYY-MM-DD).

        Returns:
            DimDate: A new DimDate instance.
        """
        # Create a copy to avoid modifying the input
        data_copy = data.copy()

        # Handle date_id conversion if it's a string
        if isinstance(data_copy.get('date_id'), str):
            data_copy['date_id'] = date.fromisoformat(data_copy['date_id'])

        # Handle boolean conversion for is_weekend and is_holiday if they're strings
        for bool_field in ['is_weekend', 'is_holiday']:
            if bool_field in data_copy and isinstance(data_copy[bool_field], str):
                data_copy[bool_field] = (data_copy[bool_field].lower()
                                         in ['true', 't', 'yes', 'y', '1'])

        # Explicitly reference the DimDate class
        return DimDate(**data_copy)

    @property
    def yearly_range(self) -> range:
        """Calculate the yearly range for this date.
        :return:
        """
        start = (self.year * 1000) + ((self.month - 1) * 31) + self.day
        return range(start, start + 1)


@dataclass
class DimTime:
    """
    DimTime dataclass.
    """
    time_id: time = field()
    hour: int = field()
    minute: int = field()
    day_night: str = field()
    time_of_day: str = field()

    # Class method decorator to ensure the method works properly with inheritance
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Self:
        """
        Create a DimTime instance from a dictionary.

        Args:
            data (dict): Dictionary containing the DimTime attributes.
                         The time_id key should contain a time string in format HH:MM:SS.

        Returns:
            DimTime: A new DimTime instance.
        """
        # Handle time_id conversion if it's a string
        if isinstance(data.get('time_id'), str):
            data = data.copy()  # Create a copy to avoid modifying the input
            data['time_id'] = time.fromisoformat(data['time_id'])

        return cls(**data)

    @staticmethod
    def from_dict_stat(data: Dict[str, Any]) -> "DimTime":
        """
        Create a DimTime instance from a dictionary.
        :param data:
        :return:
        """
        # We'd need to reference DimTime directly here
        # This creates tighter coupling and is less flexible with inheritance
        if isinstance(data.get('time_id'), str):
            data = data.copy()
            data['time_id'] = time.fromisoformat(data['time_id'])
        return DimTime(**data)  # Hardcoding the class name

    @property
    def minutes_of_day(self) -> int:
        """Calculate minutes since start of day."""
        return self.hour * 60 + self.minute


# Bridge table for multivalued relationships
@dataclass
class BridgeWeatherConditions:
    """
    A dataclass containing the BridgeWeatherConditions attributes.
    """
    weather_id: int = field(default=0)
    condition_id: int = field(default=0)
    condition_intensity: Optional[str] = field(default=None)


# Data Transfer Object for API inputs
# This class acts as a container that encapsulates weather data coming from
# external sources (like weather APIs) into a single object.
@dataclass
class WeatherDataDTO:
    """
    A dataclass containing the WeatherDataDTO attributes.
    """
    city_id: int = field(default=0)
    temperature: Decimal = field(default=Decimal('0.0'))
    feels_like: Decimal = field(default=Decimal('0.0'))
    temp_min: Decimal = field(default=Decimal('inf'))
    temp_max: Decimal = field(default=Decimal('inf'))
    comfort_level: Optional[str] = field(default=None)
    temperature_category: Optional[str] = field(default=None)
    temp_range: Optional[Decimal] = field(default=Decimal('inf'))
    pressure: int = field(default=0)
    sea_level_pressure: int = field(default=0)
    ground_level_pressure: int = field(default=0)
    pressure_trend: PressureTrend = field(default=PressureTrend.UNKNOWN)
    pressure_category: Optional[str] = field(default=None)
    is_normal_range: Optional[bool] = field(default=False)
    storm_potential: Optional[bool] = field(default=False)
    humidity: int = field(default=0)
    humidity_category: Optional[str] = field(default=None)
    comfort_impact: Optional[str] = field(default=None)
    is_dew_point_risk: Optional[bool] = field(default=False)
    mold_risk_level: Optional[str] = field(default=None)
    wind_speed: Decimal = field(default=Decimal('0.0'))
    wind_degrees: int = field(default=0)
    wind_gust: Optional[Decimal] = field(default=None)
    cloudiness: int = field(default=0)
    visibility: int = field(default=10000)
    precipitation_type: PrecipitationType = field(default=PrecipitationType.NONE)
    precipitation_volume_1h: Optional[Decimal] = field(default=None)
    precipitation_volume_3h: Optional[Decimal] = field(default=None)
    intensity_category: Optional[str] = field(default=None)
    impact_level: Optional[str] = field(default=None)
    flooding_risk: Optional[str] = field(default=None)
    weather_condition_ids: List[Any] = field(default_factory=list)
    calculation_time: datetime = field(default_factory=datetime.now)


# Factory methods to create dimension objects
class DimensionFactory:
    """
    A dataclass containing the DimensionFactory attributes.
    """

    @staticmethod
    def create_wind_dimension(
            speed: Decimal,
            degrees: int,
            gust_speed: Optional[Decimal] = None
    ) -> DimWind:
        """Factory method to create a wind dimension object without an ID (for new records).
        :param speed:
        :param degrees:
        :param gust_speed:
        :return:
        """
        return DimWind(
            wind_id=-1,  # Temporary ID, to be replaced by database
            speed=speed,
            degrees=degrees,
            gust_speed=gust_speed
        )

    @staticmethod
    def create_temperature_dimension(
            temperature: Decimal,
            feels_like: Decimal,
            temp_min: Optional[Decimal] = Decimal('inf'),
            temp_max: Optional[Decimal] = Decimal('inf'),
            comfort_level: Optional[str] = None,
            temperature_category: Optional[str] = None,
            temp_range: Optional[Decimal] = Decimal('inf')
    ) -> DimTemperature:
        """Factory method to create a temperature dimension object without an ID.
        :param temperature:
        :param feels_like:
        :param temp_min:
        :param temp_max:
        :param comfort_level:
        :param temperature_category:
        :param temp_range:
        :return:
        """
        return DimTemperature(
            temperature_id=-1,  # Temporary ID, to be replaced by database
            temperature=temperature,
            feels_like=feels_like,
            temp_min=temp_min,
            temp_max=temp_max,
            comfort_level=comfort_level,
            temperature_category=temperature_category,
            temp_range=temp_range
        )

    @staticmethod
    def create_pressure_dimension(
            pressure: int,
            sea_level_pressure: Optional[int] = None,
            ground_level_pressure: Optional[int] = None,
            pressure_trend: Optional[PressureTrend] = None,
            pressure_category: Optional[str] = None,
            is_normal_range: Optional[bool] = False,
            storm_potential: Optional[bool] = False
    ) -> DimPressure:
        """Factory method to create a pressure dimension object without an ID.
        :param pressure:
        :param sea_level_pressure:
        :param ground_level_pressure:
        :param pressure_trend:
        :param pressure_category:
        :param is_normal_range:
        :param storm_potential:
        :return:
        """
        return DimPressure(
            pressure_id=-1,  # Temporary ID, to be replaced by database
            pressure=pressure,
            sea_level_pressure=sea_level_pressure,
            ground_level_pressure=ground_level_pressure,
            pressure_trend=pressure_trend,
            pressure_category=pressure_category,
            is_normal_range=is_normal_range,
            storm_potential=storm_potential
        )

    @staticmethod
    def create_humidity_dimension(
            humidity: int,
            humidity_category: Optional[str] = None,
            comfort_impact: Optional[str] = None,
            is_dew_point_risk: Optional[bool] = False,
            mold_risk_level: Optional[str] = None
    ) -> DimHumidity:
        """Factory method to create a humidity dimension object without an ID.
        :param humidity:
        :param humidity_category:
        :param comfort_impact:
        :param is_dew_point_risk:
        :param mold_risk_level:
        :return:
        """
        return DimHumidity(
            humidity_id=-1,  # Temporary ID, to be replaced by database
            humidity=humidity,
            humidity_category=humidity_category,
            comfort_impact=comfort_impact,
            is_dew_point_risk=is_dew_point_risk,
            mold_risk_level=mold_risk_level
        )

    @staticmethod
    def create_precipitation_dimension(
            precipitation_type: PrecipitationType,
            volume_1h: Optional[Decimal] = None,
            volume_3h: Optional[Decimal] = None,
            intensity_category: Optional[str] = None,
            impact_level: Optional[str] = None,
            flooding_risk: Optional[str] = None
    ) -> DimPrecipitation:
        """Factory method to create a precipitation dimension object without an ID.
        :param precipitation_type:
        :param volume_1h:
        :param volume_3h:
        :param intensity_category:
        :param impact_level:
        :param flooding_risk:
        :return:
        """
        return DimPrecipitation(
            precipitation_id=-1,  # Temporary ID, to be replaced by database
            precipitation_type=precipitation_type,
            volume_1h=volume_1h,
            volume_3h=volume_3h,
            intensity_category=intensity_category,
            impact_level=impact_level,
            flooding_risk=flooding_risk
        )


# Data Transfer Object to Domain Object mapper
# pylint: disable=too-few-public-methods
class WeatherMapper:
    """
    A dataclass containing the WeatherMapper attributes.
    """

    @staticmethod
    def dto_to_dimensions(dto: WeatherDataDTO) -> Dict[str, Any]:
        """Convert a DTO to dimension objects.
        :param dto:
        :return:
        """
        dimensions = {
            "wind": DimensionFactory.create_wind_dimension(
                speed=dto.wind_speed,
                degrees=dto.wind_degrees,
                gust_speed=dto.wind_gust
            ),
            "temperature": DimensionFactory.create_temperature_dimension(
                temperature=dto.temperature,
                feels_like=dto.feels_like,
                temp_min=dto.temp_min,
                temp_max=dto.temp_max,
                comfort_level=dto.comfort_level,
                temperature_category=dto.temperature_category,
                temp_range=dto.temp_range
            ),
            "pressure": DimensionFactory.create_pressure_dimension(
                pressure=dto.pressure,
                sea_level_pressure=dto.sea_level_pressure,
                ground_level_pressure=dto.ground_level_pressure,
                pressure_trend=dto.pressure_trend,
                pressure_category=dto.pressure_category,
                is_normal_range=dto.is_normal_range,
                storm_potential=dto.storm_potential
            ),
            "humidity": DimensionFactory.create_humidity_dimension(
                humidity=dto.humidity,
                humidity_category=dto.humidity_category,
                comfort_impact=dto.comfort_impact,
                is_dew_point_risk=dto.is_dew_point_risk
            ),
            "precipitation": DimensionFactory.create_precipitation_dimension(
                precipitation_type=dto.precipitation_type,
                volume_1h=dto.precipitation_volume_1h,
                volume_3h=dto.precipitation_volume_3h,
                intensity_category=dto.intensity_category,
                impact_level=dto.impact_level,
                flooding_risk=dto.flooding_risk
            )
        }

        return dimensions


# Example of creating a date dimension factory
class DateDimensionFactory:
    """
    A dataclass containing the DateDimensionFactory attributes.
    """

    @staticmethod
    def create_from_date(date_value: date) -> DimDate:
        """Create a date dimension from a date object.
        :param date_value:
        :return:
        """
        return DimDate(
            date_id=date_value,
            year=date_value.year,
            quarter=(date_value.month - 1) // 3 + 1,
            month=date_value.month,
            day=date_value.day,
            day_of_week=date_value.weekday(),
            week_of_year=int(date_value.strftime("%V")),
            is_weekend=date_value.weekday() >= 5,  # 5=Saturday, 6=Sunday
            season=DateDimensionFactory._get_season(date_value),
            month_name=date_value.strftime("%B"),
            day_name=date_value.strftime("%A")
        )

    @staticmethod
    def _get_season(date_value: date) -> str:
        """Determine the season based on the date.
        :param date_value:
        :return:
        """
        month = date_value.month
        if month in (12, 1, 2):
            return "Winter"
        if month in (3, 4, 5):
            return "Spring"
        if month in (6, 7, 8):
            return "Summer"
        # month in (9, 10, 11)
        return "Fall"

    @staticmethod
    def create_date_range(start_date: date, end_date: date) -> List[DimDate]:
        """Create date dimensions for a range of dates.
        :param start_date:
        :param end_date:
        :return:
        """
        result = []
        current_date = start_date

        while current_date <= end_date:
            result.append(DateDimensionFactory.create_from_date(current_date))
            current_date += timedelta(days=1)

        return result


# Time dimension factory
class TimeDimensionFactory:
    """
    A dataclass containing the TimeDimension attributes.
    """

    @staticmethod
    def create_from_time(time_value: time) -> DimTime:
        """Create a time dimension from a time object.
        :param time_value:
        :return:
        """
        return DimTime(
            time_id=time_value,
            hour=time_value.hour,
            minute=time_value.minute,
            day_night=TimeDimensionFactory._get_day_night(time_value),
            time_of_day=TimeDimensionFactory._get_time_of_day(time_value)
        )

    @staticmethod
    def _get_day_night(time_value: time) -> str:
        """Determine if it's day or night based on time.
        :param time_value:
        :return:
        """
        if 6 <= time_value.hour <= 18:
            return "Day"
        return "Night"

    @staticmethod
    def _get_time_of_day(time_value: time) -> str:
        """Get more specific time of day description.
        :param time_value:
        :return:
        """
        hour = time_value.hour
        if 5 <= hour <= 8:
            return "Early Morning"
        if 9 <= hour <= 11:
            return "Morning"
        if 12 <= hour <= 14:
            return "Midday"
        if 15 <= hour <= 17:
            return "Afternoon"
        if 18 <= hour <= 21:
            return "Evening"
        # hour > 21
        return "Night"

    @staticmethod
    def create_hourly_times() -> List[DimTime]:
        """Create time dimensions for each hour of the day.
        :return:
        """
        return [
            TimeDimensionFactory.create_from_time(time(hour=h, minute=0))
            for h in range(24)
        ]


# Example of a composite class for analytical results
@dataclass
class WeatherAnalysis:
    """
    Weather analysis dataclass.
    """
    city_name: str = field(default="")
    country: str = field(default="")
    date: date = field(default_factory=date.today)
    avg_temperature: Decimal = field(default=Decimal('0.0'))
    min_temperature: Decimal = field(default=Decimal('0.0'))
    max_temperature: Decimal = field(default=Decimal('0.0'))
    avg_humidity: int = field(default=0)
    avg_wind_speed: Decimal = field(default=Decimal('0.0'))
    max_wind_speed: Decimal = field(default=Decimal('0.0'))
    precipitation_hours: int = field(default=0)
    total_rainfall: Decimal = field(default=Decimal('0.0'))
    total_snowfall: Decimal = field(default=Decimal('0.0'))
    weather_conditions: List[str] = field(default_factory=list)
