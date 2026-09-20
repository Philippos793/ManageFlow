import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import EmployeeForm from "../components/EmployeeForm.jsx";
import Card from "../components/ui/Card.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";
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

function EditEmployee() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState(emptyEmployee);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [pageError, setPageError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const submitLockedRef = useRef(false);

  useEffect(() => {
    employeeService
      .getEmployeeById(id)
      .then((employee) => {
        setValues({
          firstName: employee.firstName,
          lastName: employee.lastName,
          email: employee.email,
          department: employee.department,
        });
      })
      .catch((requestError) => {
        setLoadError(
          getFriendlyErrorMessage(
            requestError,
            "Failed to load this employee."
          )
        );
      })
      .finally(() => setLoading(false));
  }, [id]);

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
      .updateEmployee(id, values)
      .then(() => {
        navigate("/employees", {
          state: { success: "Employee updated successfully." },
        });
      })
      .catch((requestError) => {
        const validationErrors = getValidationErrors(requestError);

        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
        } else {
          setPageError(
            getFriendlyErrorMessage(
              requestError,
              "Failed to update employee. Please try again."
            )
          );
        }
      })
      .finally(() => {
        submitLockedRef.current = false;
        setSubmitting(false);
      });
  }

  if (loading) {
    return <LoadingSpinner label="Loading employee..." />;
  }
  if (loadError) {
    return (
      <div className="mx-auto max-w-xl space-y-5 rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <FeedbackMessage>{loadError}</FeedbackMessage>
        <Link
          to="/employees"
          className="inline-flex rounded-xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
        >
          Back to Employees
        </Link>
      </div>
    );
  }

  return (
    <section className="mx-auto max-w-4xl">
      <PageTitle
        eyebrow="Employee records"
        title="Edit Employee"
        description="Update employee information and save your changes."
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
          submitLabel="Update Employee"
          submitting={submitting}
        />
      </Card>
    </section>
  );
}

export default EditEmployee;


