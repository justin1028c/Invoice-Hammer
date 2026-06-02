package com.fordham.toolbelt.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReceiptEntity::class, 
        InvoiceEntity::class, 
        ClientEntity::class, 
        JobPhotoEntity::class, 
        JobNoteEntity::class,
        SupplierEntity::class,
        DraftInvoiceEntity::class,
        PaymentRequestEntity::class
    ], 
    version = 20,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun clientDao(): ClientDao
    abstract fun photoDao(): PhotoDao
    abstract fun jobNoteDao(): JobNoteDao
    abstract fun supplierDao(): SupplierDao
    abstract fun draftDao(): DraftDao
    abstract fun paymentRequestDao(): PaymentRequestDao
}
