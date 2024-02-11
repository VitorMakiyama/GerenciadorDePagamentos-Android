# GerenciadorDePagamentos

Development of a personal debt and payment manager, using Kotlin for Android

## Working modules:
**Main Screen - PagamentosMain:** 
A fragment that shows the list of created payments (Pagamento) (clicking in a payment shows it's information in a toast notification) and shows the details screen of the selected payment

**Creation Screen - CriarPagamento:** 
A fragment that allows you to create a new payment (Pagamento), with a Name; Start Date; Frequency; Price; and 2 Participants (more can be added by clicking on the '+'). 
If it is a one-tyime payment (no frequency) you can adjust how much each participant must pay. 
The create button only allows you to create if all the fields are properly filled.

**Details Screen - DetalhesPagamento:** 
A fragment in which you are able to visualize all the details of the selected payment (Pagamento), and have a overview of the latest payment history (HistoricoDePagamento). When you get to this screen, it will verify the payment histories for updates, based on the current date and the date of the last resgistered payment.
You can easily mark a payment history as "paid" by tapping on "not paid", which will update the entire UI to match the most recent "unpaid" payment history (if all are already "paid", the latest history will be displayed). There's a button to see all the HistoricoDePagamento of that payment ***(NYI)***.

**History Screen - HistoricosPagamento:**
A fragment that shows a list (RecyclerView) of all histories of the current payment. It allows you to mark any history as "paid", and it suggests to change all past unpaid histories to "paid". It will eventually allow you to change the value of a history. 

Projeto do Gerenciador de pagamentos - Android (Kotlin)
