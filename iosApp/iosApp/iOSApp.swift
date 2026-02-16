import SwiftUI
import BackgroundTasks
import ComposeApp

@main
struct iOSApp: App {
    private static let syncTaskIdentifier = "com.location_tracker.sync"

    init() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.syncTaskIdentifier, using: nil) { task in
            Self.handleAppRefresh(task: task as! BGAppRefreshTask)
        }

        Self.scheduleAppRefresh()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private static func handleAppRefresh(task: BGAppRefreshTask) {
        scheduleAppRefresh()

        let operationQueue = OperationQueue()
        operationQueue.maxConcurrentOperationCount = 1

        task.expirationHandler = {
            operationQueue.cancelAllOperations()
        }

        let op = BlockOperation {
            let semaphore = DispatchSemaphore(value: 0)
            BackgroundSync.shared.syncPendingLocations { _ in
                semaphore.signal()
            }
            _ = semaphore.wait(timeout: .now() + 25)
        }

        op.completionBlock = {
            task.setTaskCompleted(success: !op.isCancelled)
        }

        operationQueue.addOperation(op)
    }

    private static func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: Self.syncTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Intentionally ignored
        }
    }
}
