package com.owlting.app.stellarkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class LastLedgerSequence(val sequence: Long, @PrimaryKey val id: String = "")