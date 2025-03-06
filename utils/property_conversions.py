"""
This python script is to perform conversions for different property types.
"""


def visibility_in_km(visibility: float) -> float:
    """
    :param visibility:      visibility.

    :return:          round(self.visibility / 1000)
    """
    return round(visibility / 1000, 2)


def visibility_in_miles(visibility: float) -> float:
    """
    :param visibility:      visibility.

    :return:          round(visibility_in_km(visibility) * 0.621371)
    """
    return round(visibility_in_km(visibility) * 0.621371, 2)


def kelvin_to_celsius(kelvin: float) -> float:
    """Convert Kelvin temperature to Celsius."""
    try:
        return kelvin - 273.15
    except (TypeError, ValueError) as e:
        print(f"Error converting temperature: {e}")
        return 0.0


def fahrenheit_to_celsius(fahrenheit: float) -> float:
    """Convert Fahrenheit temperature to Celsius."""
    try:
        return (fahrenheit - 32) * 5 / 9
    except (TypeError, ValueError) as e:
        print(f"Error converting temperature: {e}")
        return 0.0


def temp_unit_conversion(temperature: float, unit: str) -> float:
    """

    :param temperature:
    :param unit:
    :return:
    """
    # Convert temperature to Celsius if needed
    temp_celsius = temperature

    unit = unit.upper()

    if unit == 'F':
        # Convert Fahrenheit to Celsius
        temp_celsius = fahrenheit_to_celsius(temperature)
    elif unit == 'K':
        # Convert Kelvin to Celsius
        temp_celsius = kelvin_to_celsius(temperature)
    elif unit != 'C':
        raise ValueError(f"Unsupported temperature unit: {unit}. Use 'C', 'F', or 'K'.")

    return temp_celsius
