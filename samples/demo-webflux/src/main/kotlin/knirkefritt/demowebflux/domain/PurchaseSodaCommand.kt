package knirkefritt.demowebflux.domain

class PurchaseSodaCommand (
    /**
     * Every vendor machine is uniquely identified by their physical location
     */
    val location: String,
    val sodaBrand: String,
)

class PurchaseSodaCommandResponse(
    val success: Boolean,
)