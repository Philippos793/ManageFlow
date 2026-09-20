import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import EmployeeForm from "../components/EmployeeForm.jsx";
import Card from "../components/ui/Card.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.jsx";
import employeeService from "../services/employeeService";
import {
  getFriendlyErrorMessage,
  getValidationErrors,
} from "../utils/apiErrors.js";

const emptyEmployee = {
  firstName: "",
  lastName: "",
  email: "",
  department: "",
};

function AddEmployee() {
  const navigate = useNavigate();
  const [values, setValues] = useState(emptyEmployee);
  const [errors, setErrors] = useState({});
  const [pageError, setPageError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [reactivation, setReactivation] = useState(null);
  const [reactivating, setReactivating] = useState(false);
  const submitLockedRef = useRef(false);
  const reactivateLockedRef = useRef(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setValues((currentValues) => ({ ...currentValues, [name]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [name]: undefined }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current) return;
    submitLockedRef.current = true;
    setErrors({});
    setPageError("");
    setSubmitting(true);

    employeeService
      .createEmployee(values)
      .then(() => {
        navigate("/employees", {
          state: { success: "Employee created successfully." },
        });
      })
      .catch((requestError) => {
        if (
          requestError?.status === 409 &&
          requestError?.data?.code === "INACTIVE_EMPLOYEE" &&
          requestError?.data?.employeeId
        ) {
          setReactivation({
            employeeId: requestError.data.employeeId,
            firstName: values.firstName,
            lastName: values.lastName,
          });
          return;
        }
        const validationErrors = getValidationErrors(requestError);

        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
        } else {
          setPageError(
            getFriendlyErrorMessage(
              requestError,
              "Failed to create employee. Please try again."
            )
          );
        }
      })
      .finally(() => {
        submitLockedRef.current = false;
        setSubmitting(false);
      });
  }

  async function handleReactivate() {
    if (!reactivation || reactivateLockedRef.current) return;
    reactivateLockedRef.current = true;
    setReactivating(true);
    setPageError("");
    try {
      await employeeService.reactivateEmployee(reactivation.employeeId, {
        firstName: reactivation.firstName,
        lastName: reactivation.lastName,
      });
      navigate("/employees", {
        state: { success: "Employee reactivated successfully." },
      });
    } catch (requestError) {
      setReactivation(null);
      setPageError(getFriendlyErrorMessage(
        requestError,
        "Failed to reactivate employee. Please try again."
      ));
    } finally {
      reactivateLockedRef.current = false;
      setReactivating(false);
    }
  }

  return (
    <section className="mx-auto max-w-4xl">
      <PageTitle
        eyebrow="Employee records"
        title="Add Employee"
        description="Create a new employee profile for your organization."
      />

      <div className="mt-6">
        <FeedbackMessage>{pageError}</FeedbackMessage>
      </div>

      <Card className="mt-7 p-6 sm:p-8">
        <EmployeeForm
          values={values}
          errors={errors}
          onChange={handleChange}
          onSubmit={handleSubmit}
          onCancel={() => navigate("/employees")}
          submitLabel="Save Employee"
          submitting={submitting}
        />
      </Card>
      <ConfirmDialog
        open={Boolean(reactivation)}
        title="Reactivate Employee?"
        message="An inactive employee with this email already exists. Would you like to reactivate the existing employee?"
        confirmLabel="Reactivate Employee"
        cancelLabel="Cancel"
        loadingLabel="Reactivating..."
        loading={reactivating}
        onConfirm={handleReactivate}
        onCancel={() => setReactivation(null)}
      />
    </section>
  );
}

export default AddEmployee;
