cmake_minimum_required(VERSION 3.22.1)

project("toolbelt-secrets")

add_library(
    toolbelt-secrets
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    toolbelt-secrets
    ${log-lib}
)
