package com.example.linkedinsigninexample.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkedInProfileModel(
    val firstName: StName,
    val lastName: StName,
    val profilePicture: ProfilePicture,
    val id: String
)

@Serializable
data class StName(
    val localized: Localized
)

@Serializable
data class Localized(
    @SerialName("en_US")
    val enUS: String
)

@Serializable
data class ProfilePicture(
    @SerialName("displayImage~")
    val displayImage: DisplayImage
)

@Serializable
data class DisplayImage(
    val elements: List<Element>
)

@Serializable
data class Element(
    val identifiers: List<Identifier>
)

@Serializable
data class Identifier(
    val identifier: String
)
