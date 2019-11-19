package com.example.linkedinsigninexample.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LinkedInEmailModel(
    val elements: List<ElementEmail>
)

@Serializable
data class ElementEmail(
    @SerialName("handle~")
    val elementHandle: Handle,

    val handle: String
)

@Serializable
data class Handle(
    val emailAddress: String
)
