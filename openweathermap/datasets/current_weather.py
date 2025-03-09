"""
This python script is for the current weather dataclasses for the openweather API.
"""
from dataclasses import dataclass, field
from datetime import date, time, datetime
from decimal import Decimal
from typing import Optional, List


# Fact Classes
# pylint: disable=too-many-instance-attributes
@dataclass
class FactCurrentWeather:
    """Class representing the Current Weather Data information"""
    weather_id: int = field(default=0)
    city_id: int = field(default=0)
    # condition_id: int = field(default=0)
    condition_ids: List[int] = field(default_factory=list)  # This is the correct way
    wind_id: int = field(default=0)
    temperature_id: int = field(default=0)
    pressure_id: int = field(default=0)
    humidity_id: int = field(default=0)
    precipitation_id: int = field(default=0)
    date_id: date = field(default_factory=date.today)
    time_id: time = field(default_factory=lambda: datetime.now().time())
    calculation_time: datetime = field(default_factory=datetime.now)
    cloudiness: int = field(default=0)
    visibility: int = field(default=0)
    latitude: Optional[Decimal] = field(default=None)
    longitude: Optional[Decimal] = field(default=None)
    created_at: datetime = field(default_factory=datetime.now)
    # Additional fields that could be degenerate dimensions
    response_code: Optional[int] = field(default=None)
    base_param: Optional[str] = field(default=None)
    source_system: Optional[str] = field(default=None)

    # This implementation:
    #
    # Handles integer fields with a common default of 0
    # Processes optional fields only if they're present in the input dictionary
    # Converts string representations to the appropriate types (date, time, datetime, Decimal)
    # Preserves existing date/time/decimal objects if they're already in the right format
    # Uses a classmethod to allow proper inheritance and subclassing
    #
    # The method is robust enough to handle various input formats for the date, time, and numeric
    # fields, making it flexible for different data sources.
    # pylint: disable=too-many-branches
    @classmethod
    def from_dict(cls, data: dict) -> "FactCurrentWeather":
        """
        Create a FactCurrentWeather instance from a dictionary.

        Args:
            data: Dictionary containing weather data

        Returns:
            A new FactCurrentWeather instance
        """
        # Integer fields with default 0
        int_fields = [
            'weather_id', 'city_id', 'condition_id', 'wind_id',
            'temperature_id', 'pressure_id', 'humidity_id',
            'precipitation_id', 'cloudiness', 'visibility'
        ]

        # Process integer fields
        processed_data = {field: data.get(field, 0) for field in int_fields}

        # Process optional integer field
        if 'response_code' in data:
            processed_data['response_code'] = data['response_code']

        # Process optional string fields
        for field_name in ['base_param', 'source_system']:
            if field_name in data:
                processed_data[field_name] = data[field_name]

        # TODO This needs proper testing # pylint: disable=fixme
        # Process date field
        # This improved version:
        #
        # 1. Properly handles the case when date_val is a string, with error handling for
        #    non-ISO formats
        # 2. Adds support for when date_val is a datetime object by extracting just the date part
        # 3. Includes a fallback to today's date if parsing fails or the type is unexpected
        # 4. Still handles the case when date_val is already a date object
        #
        # The core issue is that date.fromisoformat() is very strict about its input format.
        # If your input isn't exactly in ISO format (YYYY-MM-DD), it will raise a ValueError.
        # This correction should make your code more resilient to different input data formats.
        if 'date_id' in data:
            date_val = data['date_id']
            if isinstance(date_val, str):
                try:
                    processed_data['date_id'] = date.fromisoformat(date_val)
                except ValueError:
                    # Try to parse alternative date formats
                    try:
                        # Parse using datetime and extract the date part
                        dt = datetime.fromisoformat(date_val)
                        processed_data['date_id'] = dt.date()
                    except ValueError:
                        # Fallback to today's date if parsing fails
                        processed_data['date_id'] = date.today()
            elif isinstance(date_val, datetime):
                # Extract date part if it's a datetime object
                processed_data['date_id'] = date_val.date()
            elif isinstance(date_val, date):
                # Already a date object
                processed_data['date_id'] = date_val
            else:
                # Fallback for unexpected types
                processed_data['date_id'] = date.today()

        # TODO This needs proper testing # pylint: disable=fixme
        # Process time field
        # This improved version:
        #
        # 1. Handles string inputs with appropriate error handling, similar to the date processing
        # 2. Adds support for when time_val is a datetime object by extracting just the time part
        # 3. Includes a fallback to the current time if parsing fails or the type is unexpected
        # 4. Still handles the case when time_val is already a time object
        #
        # The time.fromisoformat() method also expects a specific format (HH:MM:SS[.ffffff]),
        # and will raise a ValueError if the input doesn't match this pattern. This correction
        # adds the same level of resilience to your time processing as we added to the date
        # processing.
        if 'time_id' in data:
            time_val = data['time_id']
            if isinstance(time_val, str):
                try:
                    processed_data['time_id'] = time.fromisoformat(time_val)
                except ValueError:
                    # Try to parse alternative time formats
                    try:
                        # Parse using datetime and extract the time part
                        dt = datetime.fromisoformat(time_val)
                        processed_data['time_id'] = dt.time()
                    except ValueError:
                        # Fallback to current time if parsing fails
                        processed_data['time_id'] = datetime.now().time()
            elif isinstance(time_val, datetime):
                # Extract time part if it's a datetime object
                processed_data['time_id'] = time_val.time()
            elif isinstance(time_val, time):
                # Already a time object
                processed_data['time_id'] = time_val
            else:
                # Fallback for unexpected types
                processed_data['time_id'] = datetime.now().time()

        # Process datetime fields
        for dt_field in ['calculation_time', 'created_at']:
            if dt_field in data:
                dt_val = data[dt_field]
                if isinstance(dt_val, str):
                    processed_data[dt_field] = datetime.fromisoformat(dt_val)
                elif isinstance(dt_val, datetime):
                    processed_data[dt_field] = dt_val

        # Process decimal fields
        for dec_field in ['latitude', 'longitude']:
            if dec_field in data and data[dec_field] is not None:
                processed_data[dec_field] = Decimal(str(data[dec_field]))

        # Create and return the instance
        return cls(**processed_data)

    # The key differences from the class method version are:
    #
    # It uses the @staticmethod decorator instead of @classmethod
    # It doesn't take cls as the first parameter
    # It explicitly uses the class name FactCurrentWeather to instantiate the object instead of
    # using cls
    #
    # The static method approach works fine if you don't plan to subclass FactCurrentWeather or if
    # subclasses don't need their own tailored version of from_dict. However, if inheritance is
    # important in your design, the class method approach is more versatile since it will correctly
    # create instances of the derived class when called from a subclass.
    @staticmethod
    def from_dict_stat(data: dict) -> "FactCurrentWeather":
        """
        Create a FactCurrentWeather instance from a dictionary.

        Args:
            data: Dictionary containing weather data

        Returns:
            A new FactCurrentWeather instance
        """
        # Integer fields with default 0
        int_fields = [
            'weather_id', 'city_id', 'condition_id', 'wind_id',
            'temperature_id', 'pressure_id', 'humidity_id',
            'precipitation_id', 'cloudiness', 'visibility'
        ]

        # Process integer fields
        processed_data = {field: data.get(field, 0) for field in int_fields}

        # Process optional integer field
        if 'response_code' in data:
            processed_data['response_code'] = data['response_code']

        # Process optional string fields
        for field_name in ['base_param', 'source_system']:
            if field_name in data:
                processed_data[field_name] = data[field_name]

        # TODO This needs proper testing # pylint: disable=fixme
        # Process date field
        # This improved version:
        #
        # 1. Properly handles the case when date_val is a string, with error handling for
        #    non-ISO formats
        # 2. Adds support for when date_val is a datetime object by extracting just the date part
        # 3. Includes a fallback to today's date if parsing fails or the type is unexpected
        # 4. Still handles the case when date_val is already a date object
        #
        # The core issue is that date.fromisoformat() is very strict about its input format.
        # If your input isn't exactly in ISO format (YYYY-MM-DD), it will raise a ValueError.
        # This correction should make your code more resilient to different input data formats.
        if 'date_id' in data:
            date_val = data['date_id']
            if isinstance(date_val, str):
                try:
                    processed_data['date_id'] = date.fromisoformat(date_val)
                except ValueError:
                    # Try to parse alternative date formats
                    try:
                        # Parse using datetime and extract the date part
                        dt = datetime.fromisoformat(date_val)
                        processed_data['date_id'] = dt.date()
                    except ValueError:
                        # Fallback to today's date if parsing fails
                        processed_data['date_id'] = date.today()
            elif isinstance(date_val, datetime):
                # Extract date part if it's a datetime object
                processed_data['date_id'] = date_val.date()
            elif isinstance(date_val, date):
                # Already a date object
                processed_data['date_id'] = date_val
            else:
                # Fallback for unexpected types
                processed_data['date_id'] = date.today()

        # TODO This needs proper testing # pylint: disable=fixme
        # Process time field
        # This improved version:
        #
        # 1. Handles string inputs with appropriate error handling, similar to the date processing
        # 2. Adds support for when time_val is a datetime object by extracting just the time part
        # 3. Includes a fallback to the current time if parsing fails or the type is unexpected
        # 4. Still handles the case when time_val is already a time object
        #
        # The time.fromisoformat() method also expects a specific format (HH:MM:SS[.ffffff]),
        # and will raise a ValueError if the input doesn't match this pattern. This correction
        # adds the same level of resilience to your time processing as we added to the date
        # processing.
        if 'time_id' in data:
            time_val = data['time_id']
            if isinstance(time_val, str):
                try:
                    processed_data['time_id'] = time.fromisoformat(time_val)
                except ValueError:
                    # Try to parse alternative time formats
                    try:
                        # Parse using datetime and extract the time part
                        dt = datetime.fromisoformat(time_val)
                        processed_data['time_id'] = dt.time()
                    except ValueError:
                        # Fallback to current time if parsing fails
                        processed_data['time_id'] = datetime.now().time()
            elif isinstance(time_val, datetime):
                # Extract time part if it's a datetime object
                processed_data['time_id'] = time_val.time()
            elif isinstance(time_val, time):
                # Already a time object
                processed_data['time_id'] = time_val
            else:
                # Fallback for unexpected types
                processed_data['time_id'] = datetime.now().time()

        # Process datetime fields
        for dt_field in ['calculation_time', 'created_at']:
            if dt_field in data:
                dt_val = data[dt_field]
                if isinstance(dt_val, str):
                    processed_data[dt_field] = datetime.fromisoformat(dt_val)
                elif isinstance(dt_val, datetime):
                    processed_data[dt_field] = dt_val

        # Process decimal fields
        for dec_field in ['latitude', 'longitude']:
            if dec_field in data and data[dec_field] is not None:
                processed_data[dec_field] = Decimal(str(data[dec_field]))

        # Create and return the instance
        return FactCurrentWeather(**processed_data)
