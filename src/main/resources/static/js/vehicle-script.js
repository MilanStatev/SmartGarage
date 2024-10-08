// Confirm delete
function confirmDelete() {
    const vehicleId = localStorage.getItem('delete_id');

    if (!jwtToken) {
        showAlert('Login to API first.', 'danger');
        return;
    }
    $('#deleteModal').modal('hide');

    // Send DELETE request to delete vehicle
    $.ajax({
        url: `/api/garage/vehicles/${vehicleId}`,
        type: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + jwtToken
        },
        success: function (response) {
            showAlert('Vehicle scraped successfully', 'success');
            setTimeout(() => {
                location.href = "/garage/vehicles";  // Reload the page to update the table
            }, 3000);
        },
        error: function (response) {
            showAlert('Error: ' + response.responseText, 'danger');
        }
    });
}

