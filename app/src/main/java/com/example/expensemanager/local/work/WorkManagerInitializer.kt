//package com.example.expensemanager.local.work
//
//
//import android.content.Context
//import androidx.hilt.work.HiltWorkerFactory
//import androidx.startup.Initializer
//import androidx.work.Configuration
//import androidx.work.WorkManager
//import dagger.hilt.EntryPoint
//import dagger.hilt.InstallIn
//import dagger.hilt.android.EntryPointAccessors
//import dagger.hilt.components.SingletonComponent
//
///**
// * Initializes WorkManager at app startup using androidx.startup.
// * This is the recommended modern approach.
// */
//class WorkManagerInitializer : Initializer<WorkManager> {
//
//    /**
//     * This is the method that will be called on app startup.
//     * It configures and initializes WorkManager.
//     */
//    override fun create(context: Context): WorkManager {
//        // We need the HiltWorkerFactory, but we can't @Inject it here.
//        // So we use an EntryPoint to get it from the Hilt graph.
//        val workerFactory = EntryPointAccessors.fromApplication(
//            context,
//            WorkManagerInitializerEntryPoint::class.java
//        ).hiltWorkerFactory()
//
//        val configuration = Configuration.Builder()
//            .setWorkerFactory(workerFactory)
//            .build()
//
//        // Initialize WorkManager with our custom configuration
//        WorkManager.initialize(context, configuration)
//
//        // Return the instance of WorkManager
//        return WorkManager.getInstance(context)
//    }
//
//    /**
//     * This initializer has no other initializers it depends on.
//     */
//    override fun dependencies(): List<Class<out Initializer<*>>> {
//        return emptyList()
//    }
//
//    /**
//     * An EntryPoint to bridge the non-Hilt Initializer with the Hilt dependency graph.
//     * This allows us to access Hilt-provided objects.
//     */
//    @EntryPoint
//    @InstallIn(SingletonComponent::class)
//    interface WorkManagerInitializerEntryPoint {
//        fun hiltWorkerFactory(): HiltWorkerFactory
//    }
//}