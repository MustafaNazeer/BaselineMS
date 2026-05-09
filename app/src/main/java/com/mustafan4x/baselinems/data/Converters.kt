package com.mustafan4x.baselinems.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromTestType(value: TestType?): String? = value?.name
    @TypeConverter fun toTestType(value: String?): TestType? = value?.let { TestType.valueOf(it) }

    @TypeConverter fun fromSex(value: Sex?): String? = value?.name
    @TypeConverter fun toSex(value: String?): Sex? = value?.let { Sex.valueOf(it) }

    @TypeConverter fun fromHand(value: Hand?): String? = value?.name
    @TypeConverter fun toHand(value: String?): Hand? = value?.let { Hand.valueOf(it) }

    @TypeConverter fun fromMSType(value: MSType?): String? = value?.name
    @TypeConverter fun toMSType(value: String?): MSType? = value?.let { MSType.valueOf(it) }
}
