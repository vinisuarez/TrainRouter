# TrainRouter
 Application Kotlin Application for train router

# Prerequisites
Docker (https://docs.docker.com/engine/install/)

# How to Run
Navigate to the root folder and run with docker:

`docker-compose up`

# APIs

1. `POST /api/v1/graph`
    ### Description:
    Upload either the station coordinates or train schedule.
    The type of file is determined  by the file name. If uploading the station coordinates
    the name must contain ``coordinates`` and for the train schedule it must contain ``schedule``
    ### Request:
    form-data = file to be loaded example: ``uk_train_schedule.csv``
    
    Content-Type:	multipart/form-data; boundary=----WebKitFormBoundaryZmSg5RXb8uEuC1n6
    
    ### Response 
    Either the Timetable id or the road graph id depending on which file was upload

    #### Response Example:
    ```
    {
    "roadGraphId" : "fd8271cc-6291-4574-8df7-57789ab32ab4"
    "timetableId" : "144702ae-4695-478f-95cb-103c0f8d2e79"
    }
    ```

2. `GET /api/v1/graph/road/{id}`
    ### Description:
    Debug APi to get the road graph (coordinates) upload for certain id
    ### Request:
    example:
    
    ``/api/v1/graph/road/fd8271cc-6291-4574-8df7-57789ab32ab4``

    ### Response
    Json with the array the road graph

    Fields
     - graphId: String
     - stationName: String
     - latitude: Double
     - longitude: Double
     - directLinks: List<String>

    #### Response Example:
    ```
    [ {
      "graphId" : "fd8271cc-6291-4574-8df7-57789ab32ab4",
      "stationName" : "Birmingham New Street",
      "latitude" : 52.4778,
      "longitude" : -1.8985,
      "directLinks" : [ "Manchester Piccadilly", "Swansea", "Manchester Piccadilly", "Newcastle" ]
    }, { ...
    ```
   
3. `GET /api/v1/graph/road/{id}`
   ### Description:
   Debug APi to get the timetable (schedule) upload for certain id
   ### Request:
   example:

   ``/api/v1/graph/timetable/144702ae-4695-478f-95cb-103c0f8d2e79``

   ### Response
   Json with the array the road graph

   Fields
    - graphId: String
    - trainNumber: String
    - fromStation: String
    - toStation: String
    - departureTime: String
    - arrivalTime: String
   #### Response Example:
    ```
    [ {
        "graphId" : "144702ae-4695-478f-95cb-103c0f8d2e79",
        "trainNumber" : "301",
        "fromStation" : "Birmingham New Street",
        "toStation" : "Norwich",
        "departureTime" : "06:00",
        "arrivalTime" : "07:51"
    }, { ...
    ```

4. `POST /api/v1/route`
    ### Description:
    Request to trigger the job to calculate the route, if the desired response is the SVG file with the
    shortest route coordinates the ``Accept`` header should contain ``image/svg+xml``.
    Otherwise, if the response is json with the fastest route using the trains and timetable the ``Accept``
    header should contain ``application/json``

    ### Request:
    Header
    - Content-Type: `application/json`
    - Accept: either `image/svg+xml` or `application/json`
   
    Fields
    - timetableId: String (mandatory for svg)
    - roadGraphId: String (mandatory for train route timetable)
    - startStation: String
    - endStation: String
    
    ```
    {
        "timetableId": "144702ae-4695-478f-95cb-103c0f8d2e79",
        "roadGraphId": "fd8271cc-6291-4574-8df7-57789ab32ab4",
        "startStation": "London King's Cross",
        "endStation" :"Swansea"
    }
    ```
    
    ### Response 
    The job id

    #### Response Example:
    ```
    {
        "jobId" : "3ef9f7ac-b7d5-4524-a138-02af1c2fd8c5"
    }
    ```

5. `GET /api/v1/route/{id}`

    ### Description:
    Fetch the result of the job calculation, if the job is not yet ready a message will say the job
    not yet ready, otherwise it will either reply with the SVG file or with the json of the route
    based on which requested route.
    ### Request:
    example:
    
    ``/api/v1/route/3ef9f7ac-b7d5-4524-a138-02af1c2fd8c5``

    #### Response Example:
    Either SVG File 

    example:


    <svg viewBox="-4.9409 -52.6251 7 10">
    <path d="M -0.1224 -51.5308 L -0.1224 -51.5308 L -2.5814 -51.4492 L -3.1791 -51.4757 L -3.9409 -51.6251 " stroke="#FF0000" stroke-width="0.03" fill="none"/>
    <text x="-0.02239999999999999" y="-51.580799999999996" font-size="0.1">London King's Cross: (-0.1224, 51.5308)</text>
    <circle cx="-0.1224" cy="-51.5308" r="0.02"/>
    <text x="-2.4814" y="-51.5492" font-size="0.1">Bristol Temple Meads: (-2.5814, 51.4492)</text>
    <circle cx="-2.5814" cy="-51.4492" r="0.02"/>
    <text x="-3.0791" y="-51.6257" font-size="0.1">Cardiff Central: (-3.1791, 51.4757)</text>
    <circle cx="-3.1791" cy="-51.4757" r="0.02"/>
    <text x="-3.8409" y="-51.825100000000006" font-size="0.1">Swansea: (-3.9409, 51.6251)</text>
    <circle cx="-3.9409" cy="-51.6251" r="0.02"/>
    </svg>

    
Or for the timetable route array for fields in the array:
  - graphId: String
  - trainNumber: String
  - fromStation: String
  - toStation: String
  - departureTime: String
  - arrivalTime: String


    [ {
        "graphId" : "e80f366a-ecb3-417e-aba0-0e64778bbe7e",
        "trainNumber" : "321",
        "fromStation" : "London King's Cross",
        "toStation" : "Swansea",
        "departureTime" : "11:09",
        "arrivalTime" : "12:56"
    } ]