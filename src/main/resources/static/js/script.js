document.addEventListener('DOMContentLoaded', function() {
    var autoDismissAlerts = document.querySelectorAll('.alert-auto-dismiss');
    autoDismissAlerts.forEach(function(alert) {
        setTimeout(function() {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 4000);
    });
});