cmake_minimum_required(VERSION 3.22.1)

project(xrenderer)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(
 ${CMAKE_SOURCE_DIR}/include
 ${CMAKE_SOURCE_DIR}/gl4es
 ${CMAKE_SOURCE_DIR}/glfw
)

# Source files
set(RENDERER_SOURCES
 xrenderer.cpp
 egl_context.cpp
 gl_state.cpp
 input_handler.cpp
 window_manager.cpp
)

# Build shared library
add_library(xrenderer SHARED ${RENDERER_SOURCES})

# Link libraries
target_link_libraries(xrenderer
 EGL
 GLESv3
 log
 android
 dl
)

# Definitions
target_compile_definitions(xrenderer
 PRIVATE
 EGL_NO_X11
 MESA_EGL_NO_X11_HEADERS
 GL_GLEXT_PROTOTYPES
)


