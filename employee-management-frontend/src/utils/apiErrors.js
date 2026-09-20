export class ApiError extends Error {
  constructor(status, data) {
    const message =
      typeof data === "string"
        ? data
        : data?.message || `Request failed with status ${status}`;

    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

export function getValidationErrors(error) {
  const validationErrors = error?.data?.validationErrors;

  if (
    error?.status === 400 &&
    validationErrors &&
    typeof validationErrors === "object" &&
    !Array.isArray(validationErrors)
  ) {
    return validationErrors;
  }

  return {};
}

export function getFriendlyErrorMessage(error, fallbackMessage) {
  if (error instanceof TypeError || error?.message === "Failed to fetch") {
    return "Unable to connect to the server. Check your connection and try again.";
  }

  if ([400, 404, 409].includes(error?.status) && error?.data?.message) {
    return error.data.message;
  }

  switch (error?.status) {
    case 401:
      return "Your session is invalid or has expired. Please sign in again.";
    case 403:
      return "You do not have permission to perform this action.";
    case 404:
      return "The requested information could not be found.";
    case 500:
      return "The server encountered a problem. Please try again later.";
    default:
      return error?.data?.message || fallbackMessage;
  }
}

