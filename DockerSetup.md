# Using Docker

## Setup docker image

### Following are the steps to build the docker image:

- Build the docker image:

  `$: docker build .`

- Find the image:

  `$: docker images`

- Tag the image:

  `$: docker image tag IMAGE_TAG aarjavi/wine_testing`

- Push the image to the repository:

  `$: docker push aarjavi/wine_testing`

_Note: Make sure the image is public. By default it is_

### Following are the steps to pull and run the docker image:

- Pull the image:

  `$: docker pull aarjavi/wine_testing`

- Run the image:

  `$: docker run --mount type=bind,source=SOURCE_FILE.csv,target=/test/test.csv aarjavi/wine_testing`
