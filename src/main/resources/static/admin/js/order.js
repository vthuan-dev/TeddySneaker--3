function exportInvoice(orderId) {
    $.ajax({
        url: '/api/admin/invoices/export/' + orderId,
        type: 'GET',
        xhrFields: {
            responseType: 'blob'
        },
        success: function(response) {
            const blob = new Blob([response], { type: 'application/pdf' });
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = downloadUrl;
            a.download = "invoice-" + orderId + ".pdf";
            document.body.appendChild(a);
            a.click();
            a.remove();
        },
        error: function(xhr) {
            toastr.error('Có lỗi xảy ra khi xuất hóa đơn');
        }
    });
} 