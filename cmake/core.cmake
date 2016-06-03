add_library(mbgl-core STATIC
    ${MBGL_CORE_FILES}
)

add_dependencies(mbgl-core
    mbgl-version-header
)

target_add_shaders(mbgl-core
    circle
    collisionbox
    debug
    fill
    icon
    line
    linepattern
    linesdfpattern
    outline
    outlinepattern
    pattern
    raster
    sdf
)

target_compile_options(mbgl-core
    PRIVATE -fPIC
    PRIVATE -fvisibility-inlines-hidden
)

target_include_directories(mbgl-core
    PUBLIC include
    PUBLIC src # TODO: make private
    PRIVATE ${CMAKE_BINARY_DIR}/include
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/include
)

target_add_mason_package(mbgl-core PUBLIC geometry)
target_add_mason_package(mbgl-core PUBLIC variant)
target_add_mason_package(mbgl-core PRIVATE unique_resource)
target_add_mason_package(mbgl-core PRIVATE rapidjson)
target_add_mason_package(mbgl-core PRIVATE boost)
target_add_mason_package(mbgl-core PRIVATE geojsonvt)
target_add_mason_package(mbgl-core PRIVATE earcut)
target_add_mason_package(mbgl-core PRIVATE protozero)

mbgl_platform_core()

create_source_groups(mbgl-core)
target_append_xcconfig(mbgl-core)
