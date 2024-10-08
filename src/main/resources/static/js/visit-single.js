// Submit the reschedule request
$('#rescheduleModal').on('show.bs.modal', function (event) {
    const button = $(event.relatedTarget);
    const vehicleId = button.data('visit-id');
    localStorage.setItem('visit_id', vehicleId);
});

$('#rescheduleModal').on('hidden.bs.modal', function (event) {
    localStorage.removeItem('visit_id');
});

function submitReschedule() {
    const visitId = localStorage.getItem('visit_id');
    const newBookedDate = document.getElementById('newBookedDate').value;
    $('#rescheduleModal').modal('hide');

    if (!jwtToken) {
        showAlert('Login to API first.', 'danger');
        return;
    }

    // Send PUT request to update visit
    $.ajax({
        url: `/api/garage/visits/${visitId}`,
        type: 'PUT',
        headers: {
            'Authorization': 'Bearer ' + jwtToken
        },
        data: {bookedDate: newBookedDate},
        success: function (response) {
            location.reload();
        },
        error: function (response) {
            showAlert('Error: ' + response.responseText, 'danger');
        }
    });
    showAlert('Visit rescheduled successfully', 'success');

}

// Confirm delete
function confirmDelete() {
    const visitId = localStorage.getItem('delete_id');

    if (!jwtToken) {
        showAlert('Login to API first.', 'danger');
        return;
    }
    $('#deleteModal').modal('hide');

    // Send DELETE request to cancel visit
    $.ajax({
        url: `/api/garage/visits/${visitId}`,
        type: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + jwtToken
        },
        success: function (response) {
            window.location.href = '/garage/clerk';
            showAlert('Visit cancelled successfully', 'success');
        },
        error: function (response) {
            showAlert('Error: ' + response.responseText, 'danger');
        }
    });
}

//Delete ORDER
$('#deleteOrderModal').on('show.bs.modal', function (event) {
    const button = $(event.relatedTarget);
    const vehicleId = button.data('delete-id');
    const visitId = button.data('visit-id');
    const userId = button.data('user-id');
    localStorage.setItem('delete_id', vehicleId);
    localStorage.setItem('visit-id', visitId);
    localStorage.setItem('user-id', userId);
});

$('#deleteOrderModal').on('hidden.bs.modal', function (event) {
    localStorage.removeItem('delete_id');
    localStorage.removeItem('visit-id');
    localStorage.removeItem('user-id');
});

function confirmOrderDelete() {
    const orderId = localStorage.getItem('delete_id');
    const userId = localStorage.getItem('user-id');
    const visitId = localStorage.getItem('visit-id');

    if (!jwtToken) {
        showAlert('Login to API first.', 'danger');
        return;
    }
    $('#deleteOrderModal').modal('hide');

    // Send DELETE request to cancel visit
    $.ajax({
        url: `/api/garage/users/${userId}/visits/${visitId}/orders/${orderId}`,
        type: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + jwtToken
        },
        success: function (response) {
            window.location.href = `/garage/visits/${visitId}`;
            showAlert('Order removed successfully', 'success');
        },
        error: function (response) {
            showAlert('Error: ' + response.responseText, 'danger');
        }
    });
}
//Delete ORDER END


$('#generateReportForm').submit(function (event) {
    event.preventDefault();

    if (!jwtToken) {
        showAlert('Login to API first.', 'danger');
        return;
    }

    const selectedVisitIds = [];
    selectedVisitIds.push($('#visitId').val());

    const targetEmail = $('#targetEmail').val();
    const exchangeCurrency = $('#currency').val();
    $('#generateReportModal').modal('hide');

    $.ajax({
        url: '/api/garage/visits/report',
        type: 'GET',
        headers: {
            'Authorization': 'Bearer ' + jwtToken
        },
        data: {
            visitIds: selectedVisitIds,
            email: targetEmail,
            exchangeCurrency: exchangeCurrency
        },
        success: function (response) {
            showAlert('Report generated and sent to ' + targetEmail, 'success');
        },
        error: function (xhr, status, error) {
            showAlert('Error generating report: ' + xhr.responseText, 'danger');
        }
    });
});

document.addEventListener('DOMContentLoaded', function() {
    const visitId = document.getElementById('visitId').value;
    const currencySelect = document.getElementById('exchangeCurrency');

    currencySelect.addEventListener('change', function() {
        const selectedCurrency = this.value;

        if (selectedCurrency) {
            window.location.href = `/garage/visits/${visitId}?exchangeCurrency=${selectedCurrency}`;
        }
    });
});