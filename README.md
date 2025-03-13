# NOAK Weather Engineering Pipeline project

This Python-based weather engineering pipeline currently retrieves current conditions and forecasts from
OpenWeatherMap. We plan to enhance the project with several capabilities, including:

- Implementing dimensional data modeling for persistent storage of weather data.
- Leveraging cloud technologies for streamlined data ingestion and processing.
- Incorporating machine learning algorithms for advanced analytics.
- Expanding data sources to include additional providers such as NOAA.

## Project Setup

This project is set to use the OpenWeatherMap API. As a result it is necessary to have an api key from OpenWeatherMap.
To set up the environment please do the following

1) Copy the .env_template file to .env
2) If you do not have an api key from OpenWeatherMap go the [website](https://openweathermap.org/) and create one. This
   will require to sign up first. Sign up [here](http://home.openweathermap.org/users/sign_up).
3) Enter the information for the below environment variable in the .env file only:
    1) API_KEY
4) Make sure the .env file is not added/committed to the remote repository.

## Environment file (.env)

The file contains the following information

- API_KEY - This is the users api key.
- CITY_FILE - The file to be used by the fetch_weather_data.py script to retrieve the information from OpenWeatherMap.
  <br>An example file named cities.csv can be
  found [here](https://github.com/bclasky1539/noakweather-engineering-pipeline/tree/main/data).
- OUTPUT_FILES - The directory where the retrieved weather information is to be stored by the fetch_weather_data.py
  script.
  <br>Example output files can be
  found [here](https://github.com/bclasky1539/noakweather-engineering-pipeline/tree/main/data/output).
- OUTPUT_BKP_FILES - The directory where the weather information is to be archived to.
  <br>Example archived output files can be
  found [here](https://github.com/bclasky1539/noakweather-engineering-pipeline/tree/main/data/output/backup).
- OWM_GEO_URL - This is the static URL for the GEO information.
- OWM_CUR_WEATHER_URL - This is the static URL for the current weather information.
- LANGUAGE - This is the language to be used for some of the information returned. Currently, it is set to english.
- UNITS_OF_MEASURE - This is the units of measurement which can be set to standard, metric or imperial. Currently, it is
  set to imperial.
- STANDARD_TEMPERATURE - This is the units for temperature in Kelvin.
- IMPERIAL_TEMPERATURE - This is the units for temperature in Fahrenheit.
- METRIC_TEMPERATURE - This is the units for temperature in Celsius.
- PRESSURE - This is the units for atmospheric pressure in hPa.
- HUMIDITY - This is the units for humidity in %.
- STANDARD_WIND_SPEED - This is the units for wind speed in meters/second.
- IMPERIAL_WIND_SPEED - This is the units for wind speed in miles/hour.
- WIND_DIRECTION - This is the units for wind direction in degrees.
- VISIBILITY - This is the units for visibility in kilometers (km). OpenWeatherMap returns it in meters but is converted
  to km. The maximum value of the visibility is 10 km.

## Issues

If you notice any problems with running this, please open an
issue [here](https://github.com/bclasky1539/noakweather-engineering-pipeline/issues).

## Contributing to the Project

- We welcome contributions through forking the repository to address issues or implement new features.
- Please include appropriate test coverage for all submitted code.
- After submission, your pull request will undergo review and testing before being merged into the main codebase.

<br>

# Updated for domain-driven design principles 

The existing project follows a conventional functional organization as outlined above. Please maintain this structure
while we progressively implement the domain-driven design architecture.

We plan to restructure the project to follow domain-driven design principles for our weather data processing pipeline.
The redesigned system will retrieve weather information from external APIs, transform this data according to our
business rules, and persist it in either PostgreSQL or Snowflake databases.

## Architecture

This project follows Domain-Driven Design (DDD) principles with a layered architecture:

- **Domain Layer**: Core business logic, models, and domain services.
- **Application Layer**: Orchestration of domain objects to fulfill use cases.
- **Infrastructure Layer**: Technical implementations of repositories and external services.
- **Interface Layer**: Entry points to the application (CLI, API).

## Project Structure

Below is the proposed project architecture. While minor adjustments may occur during implementation, we intend to
maintain this fundamental organizational structure throughout development.

```
noakweather-engineering-pipeline/
│
├── src/
│   ├── domain/                 # Domain layer with core business logic
│   │   ├── models/             # Domain entities and value objects
│   │   ├── services/           # Domain services
│   │   └── repositories/       # Repository interfaces
│   │
│   ├── application/            # Application layer (use cases)
│   │   ├── services/           # Application services
│   │   ├── dto/                # Data Transfer Objects
│   │   └── pipeline.py         # Pipeline orchestration
│   │
│   ├── infrastructure/         # Infrastructure layer
│   │   ├── repositories/       # Repository implementations
│   │   ├── external/           # External service integrations
│   │   └── persistence/        # Database connections
│   │
│   └── interfaces/             # Interface layer
│       ├── cli/                # Command-line interfaces
│       └── api/                # API interfaces (if applicable)
│
├── config/                     # Configuration
├── tests/                      # Test suite
├── scripts/                    # Utility scripts
├── docker/                     # Docker configuration
│
├── requirements.txt            # Python dependencies
├── setup.py                    # Package installation
└── README.md                   # Project documentation
```

## Domain Models

The core domain models include the following:

```
src/
└── domain/
    └── models/
        ├── init.py
        ├── common.py            # Base classes and value objects
        ├── enums.py             # All domain enums
        ├── location.py          # City and location-related entities
        ├── atmospheric.py       # Pressure, Humidity entities
        ├── weather.py           # WeatherCondition, Temperature, Wind, Precipitation
        ├── current_weather.py   # FactCurrentWeather entity
        ├── forecast.py          # FactForecast and ForecastItem entities
        └── aggregates.py        # WeatherData aggregate root
```

