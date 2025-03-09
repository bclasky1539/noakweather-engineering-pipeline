"""
This python script processes forecast data from OpenWeather API.
"""
from openweathermap.print_processor import print_forecast_results
from openweathermap.weather_processor import ForecastProcessor


def main() -> None:
    """
    Main function to process forecast data.
    """
    try:
        print("Processing forecast data...")
        processor = ForecastProcessor()
        results = processor.process()

        if results:
            print_forecast_results(results)
        else:
            print("Failed to process forecast data")

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
