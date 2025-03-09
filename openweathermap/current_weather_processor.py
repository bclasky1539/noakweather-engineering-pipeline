"""
This python script processes current weather data from OpenWeather API.
"""
from openweathermap.print_processor import print_current_weather_results
from openweathermap.weather_processor import CurrentWeatherProcessor


def main() -> None:
    """
    Main function to process current weather data.
    """
    try:
        print("Processing current weather data...")
        processor = CurrentWeatherProcessor()
        result = processor.process()

        if result:
            print_current_weather_results(result)
        else:
            print("Failed to process current weather data")

    except (ValueError, TypeError) as e:
        print(f"Error with data conversion or formatting: {e}")
    except KeyError as e:
        print(f"Missing required data key: {e}")
    except AttributeError as e:
        print(f"Object attribute error: {e}")
    except FileNotFoundError as e:
        print(f"File not found: {e}")
    except PermissionError as e:
        print(f"Permission error accessing a file: {e}")
    except ImportError as e:
        print(f"Error importing module: {e}")
    except (IOError, OSError) as e:
        print(f"I/O or OS error: {e}")


if __name__ == '__main__':
    main()
