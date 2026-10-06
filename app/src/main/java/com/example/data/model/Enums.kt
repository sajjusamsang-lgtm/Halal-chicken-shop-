package com.example.data.model

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer")
}

enum class PaymentStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    PENDING("Pending"),
    OVERDUE("Overdue")
}

enum class CustomerType(val label: String) {
    RETAIL("Retail"),
    SCHOOL("School"),
    HOTEL("Hotel"),
    RESTAURANT("Restaurant"),
    OTHER("Other")
}

enum class ProductType(val label: String) {
    CHICKEN("Chicken"),
    EGG("Eggs"),
    OTHER("Other")
}

enum class ChickenCut(val label: String) {
    WHOLE("Whole Chicken"),
    CURRY_CUT("Curry Cut"),
    BONELESS("Boneless"),
    LEG_PIECE("Leg Piece"),
    BREAST("Breast"),
    SMALL_PIECES("Small Pieces")
}

enum class WeightUnit(val label: String) {
    KG("KG"),
    GRAM("Gram"),
    PIECE("Piece")
}

enum class EggType(val label: String) {
    BROWN("Brown Eggs"),
    WHITE("White Eggs")
}

enum class EggUnit(val label: String, val eggCount: Int) {
    TRAY("Tray", 30),
    CARDBOARD("Cardboard", 210),
    PIECE("Piece", 1)
}

enum class ExpenseCategory(val label: String) {
    ELECTRICITY("Electricity"),
    RENT("Rent"),
    LABOUR("Labour"),
    SALARY("Salary"),
    TRANSPORT("Transport"),
    ICE("Ice"),
    PACKAGING("Packaging"),
    CLEANING("Cleaning"),
    EQUIPMENT("Equipment"),
    MAINTENANCE("Maintenance"),
    FUEL("Fuel"),
    PHONE_INTERNET("Phone/Internet"),
    OTHER("Other")
}

enum class PaymentCycle(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}
