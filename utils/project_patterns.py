"""
This python script is to get the class name of an object.
"""
import os
from decimal import Decimal, InvalidOperation
from typing import Any
from datetime import datetime


def get_full_class_name(obj: object) -> str | None:
    """
    :param obj:     The object to determine the class name.

    :return:        module + '.' + obj.__class__.__name__ string
    """
    module = obj.__class__.__module__
    if module is None or module == str.__class__.__module__:
        return obj.__class__.__name__
    return module + '.' + obj.__class__.__name__


def find_project_root(start_dir: str = '',
                      marker: str = 'pyproject.toml') -> str | None | Any:
    """
    Find the project root by looking for a marker file.

    :param start_dir:
    :param marker:
    :return:
    """

    start_dir = start_dir or os.getcwd()

    # Check if marker exists in current directory
    if os.path.exists(os.path.join(start_dir, marker)):
        return start_dir

    # Get the parent directory
    parent = os.path.dirname(start_dir)

    # If we've reached the root directory and haven't found the marker, return None
    if parent == start_dir:
        return None

    # Recursively search in the parent directory
    return find_project_root(parent, marker)


def safe_decimal(value: object, default: float = 0.0) -> Decimal:
    """Safely convert a value to Decimal.
    :param value:
    :param default:
    :return:
    """
    try:
        return Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError) as e:
        # logger.warning(f"Failed to convert {value} to Decimal: {e}")
        print(f"Failed to convert {value} to Decimal: {e}")
        return Decimal(str(default))


def safe_datetime(timestamp: int) -> datetime:
    """Safely convert a UNIX timestamp to datetime.
    :param timestamp:
    :return:
    """
    try:
        return datetime.fromtimestamp(timestamp)
    except (TypeError, ValueError, OSError) as e:
        # logger.warning(f"Failed to convert timestamp {timestamp}: {e}")
        print(f"Failed to convert timestamp {timestamp}: {e}")
        return datetime.now()
