// Copyright 2020 Miles Budnek
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

// Changelog:
// * Feb 17 2020 - Initial Release

import groovy.transform.Field

definition(
    name: "Fan Thermostat",
    namespace: "mbudnek",
    author: "Miles Budnek",
    description: "Thermostat to control ceiling fan speed",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPreferences", title: "Fan Thermostats", install: true, uninstall: true) {
        section {
            app(
                name: "childApps",
                appName: "Fan Thermostat Child",
                namespace: "mbudnek",
                title: "Add Fan Thermostat",
                multiple: true
            )
        }
    }
}

def uninstalled() {
    getChildApps().each { childApp ->
        deleteChildApp(childApp.id)
    }
}
