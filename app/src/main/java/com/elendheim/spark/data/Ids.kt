package com.elendheim.spark.data

import java.util.UUID

/** One place to mint the stable, unique ids used across decks, wheels and entries. */
fun newId(): String = UUID.randomUUID().toString()
